package com.ttait.subscription.external.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.SourceType;
import com.ttait.subscription.announcement.repository.AnnouncementParseRawRepository;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.external.ai.dto.PdfParseResult;
import com.ttait.subscription.external.lh.LhApiClient;
import com.ttait.subscription.external.pdf.PdfParsingService;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class NoticeImportOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(NoticeImportOrchestrator.class);

    private final LhApiClient lhApiClient;
    private final NoticeImportPersistenceService persistenceService;
    private final PdfParsingService pdfParsingService;
    private final LhImportDedupeDecisionService dedupeDecisionService;
    private final AnnouncementUnitGeocodingEnrichmentService geocodingEnrichmentService;
    private final ObjectMapper objectMapper;
    private final AnnouncementRepository announcementRepository;
    private final AnnouncementParseRawRepository announcementParseRawRepository;

    public NoticeImportOrchestrator(LhApiClient lhApiClient,
                                      NoticeImportPersistenceService persistenceService,
                                      PdfParsingService pdfParsingService,
                                      LhImportDedupeDecisionService dedupeDecisionService,
                                      AnnouncementUnitGeocodingEnrichmentService geocodingEnrichmentService,
                                      ObjectMapper objectMapper,
                                      AnnouncementRepository announcementRepository,
                                      AnnouncementParseRawRepository announcementParseRawRepository) {
        this.lhApiClient = lhApiClient;
        this.persistenceService = persistenceService;
        this.pdfParsingService = pdfParsingService;
        this.dedupeDecisionService = dedupeDecisionService;
        this.geocodingEnrichmentService = geocodingEnrichmentService;
        this.objectMapper = objectMapper;
        this.announcementRepository = announcementRepository;
        this.announcementParseRawRepository = announcementParseRawRepository;
    }

    public record ImportResult(
            int imported,
            int failed,
            @JsonIgnore int fetched,
            @JsonIgnore int scanned,
            @JsonIgnore int skippedLand,
            @JsonIgnore int skippedCommercial,
            @JsonIgnore int unchanged,
            @JsonIgnore int geminiSkipped,
            @JsonIgnore int reparsed,
            @JsonIgnore boolean endOfList
    ) {

        public ImportResult(int imported, int failed) {
            this(imported, failed, 0, 0, 0, 0, 0, 0, 0, false);
        }

        public ImportResult(int imported,
                            int failed,
                            int fetched,
                            int scanned,
                            int skippedLand,
                            int unchanged,
                            int geminiSkipped,
                            int reparsed,
                            boolean endOfList) {
            this(imported, failed, fetched, scanned, skippedLand, 0, unchanged, geminiSkipped, reparsed, endOfList);
        }
    }

    public record PreparedLhNotice(JsonNode item, JsonNode detailResponse) {
    }

    public record CandidateScanResult(LhImportDedupeDecision decision, String pdfUrl) {
    }

    enum ImportMode {
        SCHEDULER(false),
        LEGACY_ADMIN(false),
        SELECTED_ADMIN(false),
        FORCE_ADMIN(true);

        private final boolean force;

        ImportMode(boolean force) {
            this.force = force;
        }
    }

    private record ImportOptions(ImportMode mode, boolean force) {

        private static ImportOptions from(ImportMode mode) {
            ImportMode resolvedMode = mode == null ? ImportMode.LEGACY_ADMIN : mode;
            return new ImportOptions(resolvedMode, resolvedMode.force);
        }
    }

    public ImportResult importLhNotices(int page, int size) {
        return importLhNotices(page, size, ImportMode.LEGACY_ADMIN);
    }

    public ImportResult importLhNoticesForScheduler(int page, int size) {
        return importLhNotices(page, size, ImportMode.SCHEDULER);
    }

    public ImportResult importPreparedLhNotices(List<PreparedLhNotice> notices, boolean force) {
        if (notices == null || notices.isEmpty()) {
            return new ImportResult(0, 0, 0, 0, 0, 0, 0, 0, 0, false);
        }

        ImportOptions options = new ImportOptions(force ? ImportMode.FORCE_ADMIN : ImportMode.SELECTED_ADMIN, force);
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicInteger skippedLand = new AtomicInteger(0);
        AtomicInteger skippedCommercial = new AtomicInteger(0);
        AtomicInteger unchanged = new AtomicInteger(0);
        AtomicInteger geminiSkipped = new AtomicInteger(0);
        AtomicInteger reparsed = new AtomicInteger(0);

        for (PreparedLhNotice notice : notices) {
            try {
                CandidateScanResult scan = scanLhCandidate(notice.item(), notice.detailResponse(), force);
                if (scan.decision().decision() == LhImportDecisionType.LAND_SKIP) {
                    skippedLand.incrementAndGet();
                    continue;
                }
                if (scan.decision().decision() == LhImportDecisionType.COMMERCIAL_SKIP) {
                    skippedCommercial.incrementAndGet();
                    continue;
                }

                ImportItemOutcome outcome = processPreparedLhItem(notice.item(), notice.detailResponse(), options);
                if (outcome.imported()) {
                    imported.incrementAndGet();
                }
                if (outcome.unchanged()) {
                    unchanged.incrementAndGet();
                }
                if (outcome.geminiSkipped()) {
                    geminiSkipped.incrementAndGet();
                }
                if (outcome.reparsed()) {
                    reparsed.incrementAndGet();
                }
            } catch (Exception e) {
                String panId = notice.item() == null ? "unknown" : notice.item().path("PAN_ID").asText("unknown");
                log.error("Failed to process prepared LH notice panId={} mode={}", panId, options.mode(), e);
                failed.incrementAndGet();
            }
        }

        int scanned = notices.size();
        return new ImportResult(
                imported.get(),
                failed.get(),
                scanned,
                scanned,
                skippedLand.get(),
                skippedCommercial.get(),
                unchanged.get(),
                geminiSkipped.get(),
                reparsed.get(),
                false
        );
    }

    public CandidateScanResult scanLhCandidate(JsonNode item, JsonNode detailResponse, boolean force) {
        String pdfUrl = extractPdfUrl(detailResponse);
        return new CandidateScanResult(dedupeDecisionService.decide(item, detailResponse, pdfUrl, force), pdfUrl);
    }

    ImportResult importLhNotices(int page, int size, ImportMode mode) {
        ImportOptions options = ImportOptions.from(mode);
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicInteger scanned = new AtomicInteger(0);
        AtomicInteger skippedLand = new AtomicInteger(0);
        AtomicInteger skippedCommercial = new AtomicInteger(0);
        AtomicInteger unchanged = new AtomicInteger(0);
        AtomicInteger geminiSkipped = new AtomicInteger(0);
        AtomicInteger reparsed = new AtomicInteger(0);

        try {
            JsonNode response = lhApiClient.fetchNoticeList(page, size);
            JsonNode dsList = persistenceService.findArray(response, "dsList");

            if (dsList == null) {
                log.warn("LH API returned empty dsList for page={}", page);
                return new ImportResult(0, 0, 0, 0, 0, 0, 0, 0, 0, true);
            }

            if (dsList.isEmpty()) {
                return new ImportResult(0, 0, 0, 0, 0, 0, 0, 0, 0, true);
            }

            for (JsonNode item : dsList) {
                scanned.incrementAndGet();
                LhImportDedupeDecision skipDecision = dedupeDecisionService.decide(item, null, null, options.force());
                if (skipDecision.decision() == LhImportDecisionType.LAND_SKIP) {
                    skippedLand.incrementAndGet();
                    log.debug("Skipping LH notice panId={} decision={} reason={}",
                            skipDecision.panId(), skipDecision.decision(), skipDecision.reason());
                    continue;
                }
                if (skipDecision.decision() == LhImportDecisionType.COMMERCIAL_SKIP) {
                    skippedCommercial.incrementAndGet();
                    log.debug("Skipping LH notice panId={} decision={} reason={}",
                            skipDecision.panId(), skipDecision.decision(), skipDecision.reason());
                    continue;
                }
                try {
                    ImportItemOutcome outcome = processLhItem(item, options);
                    if (outcome.imported()) {
                        imported.incrementAndGet();
                    }
                    if (outcome.unchanged()) {
                        unchanged.incrementAndGet();
                    }
                    if (outcome.geminiSkipped()) {
                        geminiSkipped.incrementAndGet();
                    }
                    if (outcome.reparsed()) {
                        reparsed.incrementAndGet();
                    }
                } catch (Exception e) {
                    String panId = item.path("PAN_ID").asText("unknown");
                    log.error("Failed to process LH notice panId={} mode={}", panId, options.mode(), e);
                    failed.incrementAndGet();
                }
            }

        } catch (Exception e) {
            log.error("LH list fetch failed for page={}", page, e);
        }

        return new ImportResult(
                imported.get(),
                failed.get(),
                scanned.get(),
                scanned.get(),
                skippedLand.get(),
                skippedCommercial.get(),
                unchanged.get(),
                geminiSkipped.get(),
                reparsed.get(),
                false
        );
    }

    private ImportItemOutcome processLhItem(JsonNode item, ImportOptions options) {
        String panId = persistenceService.text(item, "PAN_ID");
        String ccrCnntSysDsCd = persistenceService.text(item, "CCR_CNNT_SYS_DS_CD");
        String splInfTpCd = persistenceService.text(item, "SPL_INF_TP_CD");

        if (panId == null || ccrCnntSysDsCd == null || splInfTpCd == null) {
            log.warn("Missing LH detail params for panId={}", panId);
            return ImportItemOutcome.skippedGemini();
        }

        JsonNode detailResponse = lhApiClient.fetchNoticeDetail(panId, ccrCnntSysDsCd, splInfTpCd);
        return processPreparedLhItem(item, detailResponse, options);
    }

    private ImportItemOutcome processPreparedLhItem(JsonNode item, JsonNode detailResponse, ImportOptions options) {
        String panId = persistenceService.text(item, "PAN_ID");
        CandidateScanResult scan = scanLhCandidate(item, detailResponse, options.force());
        LhImportDedupeDecision decision = scan.decision();
        String pdfUrl = scan.pdfUrl();
        if (!decision.shouldPersistOfficial() || shouldPreserveParsedDataWithoutParsing(decision)) {
            dedupeDecisionService.recordChecked(decision);
            log.info("Skipping LH import persistence panId={} mode={} decision={} reason={}",
                    panId, options.mode(), decision.decision(), decision.reason());
            return ImportItemOutcome.skipped(decision);
        }

        Announcement announcement = persistenceService.upsertLh(item);
        PdfParseResult pdfResult = null;
        String pdfRawJson = null;

        if (decision.shouldParseGemini()) {
            log.info("PDF found for panId={}: {}", panId, pdfUrl);
            try {
                pdfResult = pdfParsingService.parse(pdfUrl);
                if (pdfResult != null) {
                    pdfRawJson = safeJson(pdfResult);
                }
            } catch (RuntimeException e) {
                dedupeDecisionService.recordFailure(announcement, decision, e.getMessage());
                throw e;
            }
        }

        persistenceService.upsertLhDetail(panId, detailResponse, pdfResult, pdfRawJson);
        enrichUnitsAfterImport(announcement);
        if (decision.shouldParseGemini() && pdfResult == null) {
            dedupeDecisionService.recordFailure(announcement, decision, "Gemini parse returned no result");
        } else {
            dedupeDecisionService.recordSuccess(announcement, decision, pdfRawJson);
        }
        return ImportItemOutcome.imported(decision);
    }

    private record ImportItemOutcome(boolean imported, boolean unchanged, boolean geminiSkipped, boolean reparsed) {

        private static ImportItemOutcome skipped(LhImportDedupeDecision decision) {
            boolean unchanged = decision.decision() == LhImportDecisionType.UNCHANGED_SKIP_GEMINI;
            return new ImportItemOutcome(false, unchanged, !decision.shouldParseGemini(), false);
        }

        private static ImportItemOutcome skippedGemini() {
            return new ImportItemOutcome(false, false, true, false);
        }

        private static ImportItemOutcome imported(LhImportDedupeDecision decision) {
            boolean reparsed = decision.decision() == LhImportDecisionType.CHANGED_REPARSE
                    || decision.decision() == LhImportDecisionType.FAILED_RETRY
                    || decision.decision() == LhImportDecisionType.FORCE_REPARSE;
            return new ImportItemOutcome(true, false, !decision.shouldParseGemini(), reparsed);
        }
    }

    private boolean shouldPreserveParsedDataWithoutParsing(LhImportDedupeDecision decision) {
        return decision.preserveExistingParsedData() && !decision.shouldParseGemini();
    }

    private String extractPdfUrl(JsonNode detailResponse) {
        JsonNode attachments = persistenceService.findArray(detailResponse, "dsAhflInfo");
        if (attachments == null) return null;

        for (JsonNode node : attachments) {
            String url = persistenceService.text(node, "AHFL_URL");
            String name = persistenceService.text(node, "CMN_AHFL_NM");
            if (url == null || "다운로드".equals(url)) continue;

            String combined = ((name == null ? "" : name) + " " + url).toLowerCase();
            if (combined.contains(".pdf") || combined.contains("pdf")) {
                return url;
            }
        }
        return null;
    }

    public void reimportAnnouncement(Long announcementId) {
        Announcement announcement = announcementRepository.findByIdAndDeletedFalse(announcementId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "announcement not found: " + announcementId));

        if (announcement.getSourcePrimary() != SourceType.LH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "reimport only supported for LH source");
        }

        String itemJson = announcementParseRawRepository
                .findByAnnouncementIdAndRawType(announcementId, "LH_ITEM_JSON")
                .map(raw -> raw.getRawText())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "LH_ITEM_JSON not found for announcement: " + announcementId));

        JsonNode item;
        try {
            item = objectMapper.readTree(itemJson);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to parse stored LH_ITEM_JSON");
        }

        String panId = persistenceService.text(item, "PAN_ID");
        String ccrCnntSysDsCd = persistenceService.text(item, "CCR_CNNT_SYS_DS_CD");
        String splInfTpCd = persistenceService.text(item, "SPL_INF_TP_CD");

        if (panId == null) {
            panId = announcement.getSourceNoticeId();
        }

        JsonNode detailResponse = lhApiClient.fetchNoticeDetail(panId, ccrCnntSysDsCd, splInfTpCd);
        String pdfUrl = extractPdfUrl(detailResponse);
        LhImportDedupeDecision decision = dedupeDecisionService.decide(item, detailResponse, pdfUrl, true);
        PdfParseResult pdfResult = null;
        String pdfRawJson = null;

        if (decision.shouldParseGemini()) {
            log.info("Reimport PDF for announcementId={}: {}", announcementId, pdfUrl);
            try {
                pdfResult = pdfParsingService.parse(pdfUrl);
                if (pdfResult != null) {
                    pdfRawJson = safeJson(pdfResult);
                }
            } catch (RuntimeException e) {
                dedupeDecisionService.recordFailure(announcement, decision, e.getMessage());
                throw e;
            }
        }

        persistenceService.upsertLhDetail(panId, detailResponse, pdfResult, pdfRawJson);
        enrichUnitsAfterImport(announcement);
        if (decision.shouldParseGemini() && pdfResult == null) {
            dedupeDecisionService.recordFailure(announcement, decision, "Gemini parse returned no result");
        } else {
            dedupeDecisionService.recordSuccess(announcement, decision, pdfRawJson);
        }
    }

    private void enrichUnitsAfterImport(Announcement announcement) {
        try {
            geocodingEnrichmentService.enrichNotRequestedUnits(announcement.getId());
        } catch (RuntimeException e) {
            log.warn("Post-import geocoding enrichment failed announcementId={}", announcement.getId(), e);
        }
    }

    private String safeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
