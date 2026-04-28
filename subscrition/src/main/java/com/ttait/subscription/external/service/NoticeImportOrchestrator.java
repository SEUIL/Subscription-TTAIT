package com.ttait.subscription.external.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
    private final ObjectMapper objectMapper;
    private final AnnouncementRepository announcementRepository;
    private final AnnouncementParseRawRepository announcementParseRawRepository;

    public NoticeImportOrchestrator(LhApiClient lhApiClient,
                                    NoticeImportPersistenceService persistenceService,
                                    PdfParsingService pdfParsingService,
                                    ObjectMapper objectMapper,
                                    AnnouncementRepository announcementRepository,
                                    AnnouncementParseRawRepository announcementParseRawRepository) {
        this.lhApiClient = lhApiClient;
        this.persistenceService = persistenceService;
        this.pdfParsingService = pdfParsingService;
        this.objectMapper = objectMapper;
        this.announcementRepository = announcementRepository;
        this.announcementParseRawRepository = announcementParseRawRepository;
    }

    public record ImportResult(int imported, int failed) {
    }

    public ImportResult importLhNotices(int page, int size) {
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        try {
            JsonNode response = lhApiClient.fetchNoticeList(page, size);
            JsonNode dsList = persistenceService.findArray(response, "dsList");

            if (dsList == null) {
                log.warn("LH API returned empty dsList for page={}", page);
                return new ImportResult(0, 0);
            }

            for (JsonNode item : dsList) {
                try {
                    processLhItem(item);
                    imported.incrementAndGet();
                } catch (Exception e) {
                    String panId = item.path("PAN_ID").asText("unknown");
                    log.error("Failed to process LH notice panId={}", panId, e);
                    failed.incrementAndGet();
                }
            }

        } catch (Exception e) {
            log.error("LH list fetch failed for page={}", page, e);
        }

        return new ImportResult(imported.get(), failed.get());
    }

    private void processLhItem(JsonNode item) {
        Announcement announcement = persistenceService.upsertLh(item);

        String panId = persistenceService.text(item, "PAN_ID");
        String ccrCnntSysDsCd = persistenceService.text(item, "CCR_CNNT_SYS_DS_CD");
        String splInfTpCd = persistenceService.text(item, "SPL_INF_TP_CD");

        if (panId == null || ccrCnntSysDsCd == null || splInfTpCd == null) {
            log.warn("Missing LH detail params for panId={}", panId);
            return;
        }

        JsonNode detailResponse = lhApiClient.fetchNoticeDetail(panId, ccrCnntSysDsCd, splInfTpCd);

        // PDF 첨부파일 URL 추출
        String pdfUrl = extractPdfUrl(detailResponse);
        PdfParseResult pdfResult = null;
        String pdfRawJson = null;

        if (pdfUrl != null) {
            log.info("PDF found for panId={}: {}", panId, pdfUrl);
            pdfResult = pdfParsingService.parse(pdfUrl);
            if (pdfResult != null) {
                pdfRawJson = safeJson(pdfResult);
            }
        }

        persistenceService.upsertLhDetail(panId, detailResponse, pdfResult, pdfRawJson);
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
        PdfParseResult pdfResult = null;
        String pdfRawJson = null;

        if (pdfUrl != null) {
            log.info("Reimport PDF for announcementId={}: {}", announcementId, pdfUrl);
            pdfResult = pdfParsingService.parse(pdfUrl);
            if (pdfResult != null) {
                pdfRawJson = safeJson(pdfResult);
            }
        }

        persistenceService.upsertLhDetail(panId, detailResponse, pdfResult, pdfRawJson);
    }

    private String safeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
