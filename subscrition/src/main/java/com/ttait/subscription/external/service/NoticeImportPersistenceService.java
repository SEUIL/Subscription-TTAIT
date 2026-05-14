package com.ttait.subscription.external.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementCategory;
import com.ttait.subscription.announcement.domain.AnnouncementDetail;
import com.ttait.subscription.announcement.domain.AnnouncementEligibility;
import com.ttait.subscription.announcement.domain.AnnouncementParseRaw;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.announcement.domain.AnnouncementUnitSource;
import com.ttait.subscription.announcement.domain.ConfidenceLevel;
import com.ttait.subscription.announcement.domain.MatchSource;
import com.ttait.subscription.announcement.domain.MaritalTargetType;
import com.ttait.subscription.announcement.domain.SourceType;
import com.ttait.subscription.announcement.repository.AnnouncementCategoryRepository;
import com.ttait.subscription.announcement.repository.AnnouncementDetailRepository;
import com.ttait.subscription.announcement.repository.AnnouncementEligibilityRepository;
import com.ttait.subscription.announcement.repository.AnnouncementParseRawRepository;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.announcement.repository.AnnouncementUnitRepository;
import com.ttait.subscription.external.ai.dto.PdfParseResult;
import com.ttait.subscription.external.support.AnnouncementNormalizer;
import com.ttait.subscription.external.support.CategoryDetector;
import com.ttait.subscription.external.support.DateParsers;
import com.ttait.subscription.external.support.SupplyCountParser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoticeImportPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(NoticeImportPersistenceService.class);

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementDetailRepository announcementDetailRepository;
    private final AnnouncementCategoryRepository announcementCategoryRepository;
    private final AnnouncementParseRawRepository announcementParseRawRepository;
    private final AnnouncementEligibilityRepository announcementEligibilityRepository;
    private final AnnouncementUnitRepository announcementUnitRepository;
    private final AnnouncementNormalizer normalizer;
    private final CategoryDetector categoryDetector;
    private final SupplyCountParser supplyCountParser;
    private final LhUnitCandidateExtractor lhUnitCandidateExtractor;
    private final AnnouncementUnitSummaryService unitSummaryService;
    private final ObjectMapper objectMapper;

    public NoticeImportPersistenceService(AnnouncementRepository announcementRepository,
                                          AnnouncementDetailRepository announcementDetailRepository,
                                           AnnouncementCategoryRepository announcementCategoryRepository,
                                           AnnouncementParseRawRepository announcementParseRawRepository,
                                           AnnouncementEligibilityRepository announcementEligibilityRepository,
                                           AnnouncementUnitRepository announcementUnitRepository,
                                           AnnouncementNormalizer normalizer,
                                           CategoryDetector categoryDetector,
                                           SupplyCountParser supplyCountParser,
                                           LhUnitCandidateExtractor lhUnitCandidateExtractor,
                                           AnnouncementUnitSummaryService unitSummaryService,
                                           ObjectMapper objectMapper) {
        this.announcementRepository = announcementRepository;
        this.announcementDetailRepository = announcementDetailRepository;
        this.announcementCategoryRepository = announcementCategoryRepository;
        this.announcementParseRawRepository = announcementParseRawRepository;
        this.announcementEligibilityRepository = announcementEligibilityRepository;
        this.announcementUnitRepository = announcementUnitRepository;
        this.normalizer = normalizer;
        this.categoryDetector = categoryDetector;
        this.supplyCountParser = supplyCountParser;
        this.lhUnitCandidateExtractor = lhUnitCandidateExtractor;
        this.unitSummaryService = unitSummaryService;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Announcement upsertLh(JsonNode item) {
        String panId = text(item, "PAN_ID");
        LocalDate announcementDate = DateParsers.parseDate(text(item, "PAN_NT_ST_DT"));
        LocalDate endDate = DateParsers.parseDate(text(item, "CLSG_DT"));
        String noticeName = text(item, "PAN_NM");
        String matchKey = normalizer.buildMatchKey(panId, "LH", noticeName, null, endDate);
        String mergedGroupKey = panId == null ? null : "LH:" + panId;
        AnnouncementStatus status = normalizer.calculateStatus(null, endDate, text(item, "PAN_SS"));

        Announcement announcement = findCanonicalSourceAnnouncement(SourceType.LH, panId);
        if (announcement == null) {
            announcement = Announcement.builder()
                    .sourcePrimary(SourceType.LH)
                    .sourceNoticeId(panId)
                    .noticeName(noticeName)
                    .providerName("LH")
                    .sourceNoticeUrl(text(item, "DTL_URL"))
                    .sourceMobileUrl(text(item, "DTL_URL_MOB"))
                    .noticeStatusRaw(text(item, "PAN_SS"))
                    .noticeStatus(status)
                    .announcementDate(announcementDate)
                    .applicationEndDate(endDate)
                    .regionLevel1(text(item, "CNP_CD_NM"))
                    .supplyTypeRaw(text(item, "AIS_TP_CD_NM"))
                    .supplyTypeNormalized(normalizer.normalizeSupplyType(text(item, "AIS_TP_CD_NM")))
                    .matchKey(matchKey)
                    .merged(false)
                    .mergedGroupKey(mergedGroupKey)
                    .collectedAt(LocalDateTime.now())
                    .build();
        }

        announcement.updateFromImport(
                noticeName, "LH",
                text(item, "DTL_URL"), announcement.getSourcePcUrl(), text(item, "DTL_URL_MOB"),
                text(item, "PAN_SS"), status,
                announcementDate, null, endDate, null,
                text(item, "CNP_CD_NM"), announcement.getRegionLevel2(),
                announcement.getFullAddress(), announcement.getLegalCode(),
                announcement.getComplexName(), announcement.getProviderComplexHouseholdCount(),
                text(item, "AIS_TP_CD_NM"), normalizer.normalizeSupplyType(text(item, "AIS_TP_CD_NM")),
                announcement.getHouseTypeRaw(), announcement.getHouseTypeNormalized(),
                announcement.getDepositAmount(), announcement.getMonthlyRentAmount(),
                announcement.getSupplyHouseholdCount(),
                matchKey, false, mergedGroupKey, LocalDateTime.now()
        );

        announcement = announcementRepository.save(announcement);
        reconcileMergeGroup(announcement);
        saveParseRaw(announcement, "LH_ITEM_JSON", safeJson(item));
        return announcement;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsertLhDetail(String panId, JsonNode response, PdfParseResult pdfResult, String pdfRawJson) {
        Announcement announcement = findCanonicalSourceAnnouncement(SourceType.LH, panId);
        if (announcement != null) {
                    JsonNode schedule = first(findArray(response, "dsSplScdl"));
                    JsonNode site = first(findArray(response, "dsSbd"));
                    JsonNode etc = first(findArray(response, "dsEtcInfo"));
                    JsonNode contact = first(findArray(response, "dsCtrtPlc"));

                    AnnouncementDetail detail = announcementDetailRepository
                            .findByAnnouncementIdAndDeletedFalse(announcement.getId())
                            .orElseGet(() -> AnnouncementDetail.builder()
                                    .announcement(announcement)
                                    .build());

                    LocalDate startDate = DateParsers.parseDate(text(schedule, "SBSC_ACP_ST_DT"));
                    announcement.updateApplicationStartDate(startDate);

                    String addr = text(site, "LGDN_ADR");
                    String lccNm = text(site, "LCC_NT_NM");
                    String complexNm = lccNm != null ? lccNm : text(site, "SBD_NM"); // 상가는 SBD_NM 사용
                    if (addr == null && "전국".equals(announcement.getRegionLevel1())) {
                        addr = "전국공고(직접확인필요)";
                    }
                    announcement.updateAddress(addr, complexNm);
                    announcementRepository.save(announcement);

                    detail.updateFromImport(
                            text(schedule, "ACP_DTTM"),
                            DateParsers.parseDate(text(schedule, "PPR_ACP_ST_DT")),
                            DateParsers.parseDate(text(schedule, "PPR_ACP_CLSG_DT")),
                            DateParsers.parseDate(text(schedule, "CTRT_ST_DT")),
                            DateParsers.parseDate(text(schedule, "CTRT_ED_DT")),
                            text(site, "LCC_NT_NM"),
                            text(site, "LGDN_ADR"),
                            text(site, "LGDN_DTL_ADR"),
                            intValue(site, "HSH_CNT"),
                            text(site, "HTN_FMLA_DESC"),
                            text(site, "DDO_AR"),
                            parseSingleArea(text(site, "DDO_AR")),
                            text(site, "MVIN_XPC_YM"),
                            text(etc, "ETC_CTS"),
                            text(contact, "SIL_OFC_TLNO"),
                            text(contact, "CTRT_PLC_ADR"),
                            text(contact, "SIL_OFC_GUD_FCTS")
                    );

                    // PDF 파싱 결과 반영
                    if (pdfResult != null) {
                        String supplyRaw = valueOf(pdfResult.supplyHouseholdCount());
                        SupplyCountParser.ParseResult supplyParsed = supplyCountParser.parse(supplyRaw);

                        String eligibilityRawText = pdfResult.eligibility() != null
                                ? pdfResult.eligibility().eligibilityRaw() : null;

                        detail.updatePdfParseResult(
                                supplyRaw,
                                supplyParsed.basis(),
                                supplyParsed.confidence(),
                                valueOf(pdfResult.depositMonthlyRent()),
                                pdfResult.eligibility() != null ? pdfResult.eligibility().incomeAssetCriteriaRaw() : null,
                                valueOf(pdfResult.contact()),
                                buildEligibilityText(pdfResult),
                                pdfResult.noticeType(),
                                valueOf(pdfResult.salePriceRaw()),
                                serializeSchedules(pdfResult.scheduleDetails()),
                                valueOf(pdfResult.importantNotes())
                        );
                        detail.updateEligibilityRaw(eligibilityRawText);

                        // supply_household_count 업데이트 (HIGH 신뢰도일 때만)
                        if (supplyParsed.count() != null && supplyParsed.confidence() == ConfidenceLevel.HIGH) {
                            announcement.updateSupplyHouseholdCount(supplyParsed.count());
                        }
                        // deposit / monthly_rent 업데이트
                        announcement.updateDepositAndRent(pdfResult.depositAmountManwon(), pdfResult.monthlyRentAmountManwon());
                        // house_type_raw: LH API에 없으므로 PDF 파싱 결과로 채움
                        if (pdfResult.houseType() != null) announcement.updateHouseType(pdfResult.houseType());
                        // address 보완: API dsSbd에서 못 채운 경우(상가/집주인임대) PDF 주소로 보완
                        if (pdfResult.address() != null && announcement.getFullAddress() == null) {
                            announcement.updateAddress(pdfResult.address(), null);
                        }
                        announcementRepository.save(announcement);

                        // eligibility 저장
                        saveEligibility(announcement, pdfResult);
                    } else {
                        // PDF 파싱 실패 시에도 PENDING 상태로 검수 큐에 등록
                        // 관리자가 REIMPORT 액션으로 재파싱 가능
                        announcementEligibilityRepository.findByAnnouncementId(announcement.getId())
                                .ifPresentOrElse(
                                        existing -> {}, // 이미 존재하면 건드리지 않음
                                        () -> announcementEligibilityRepository.save(
                                                AnnouncementEligibility.builder()
                                                        .announcement(announcement)
                                                        .build()
                                        )
                                );
                    }

                    announcementDetailRepository.save(detail);

                    // raw 저장 (API JSON)
                    saveParseRaw(announcement, "API_JSON", safeJson(response));
                    // raw 저장 (PDF AI JSON)
                    if (pdfRawJson != null) {
                        saveParseRaw(announcement, "PDF_AI_JSON", pdfRawJson);
                    }

                    replaceUnits(announcement, panId, response, pdfResult);
                    unitSummaryService.applySummary(announcement);
                    announcementRepository.save(announcement);

                    // 카테고리 감지
                    String combinedText = buildCombinedText(announcement, detail, pdfResult);
                    saveCategories(announcement, combinedText);
        }
    }

    record SourcePairDuplicatePreflight(SourceType sourcePrimary, String sourceNoticeId,
                                        Announcement canonical, List<Announcement> duplicates) {

        boolean hasDuplicates() {
            return !duplicates.isEmpty();
        }

        List<Long> duplicateIds() {
            return duplicates.stream()
                    .map(Announcement::getId)
                    .toList();
        }
    }

    private Announcement findCanonicalSourceAnnouncement(SourceType sourcePrimary, String sourceNoticeId) {
        SourcePairDuplicatePreflight preflight = preflightSourcePairDuplicates(sourcePrimary, sourceNoticeId);
        if (preflight.canonical() == null) {
            return null;
        }

        Announcement canonical = preflight.canonical();
        if (preflight.hasDuplicates()) {
            List<Announcement> duplicates = preflight.duplicates();
            log.warn("Detected duplicate announcement source pair sourcePrimary={} sourceNoticeId={} canonicalId={} duplicateIds={}",
                    sourcePrimary, sourceNoticeId, canonical.getId(), preflight.duplicateIds());
            duplicates.forEach(Announcement::retireDuplicateSourceIdentity);
            announcementRepository.saveAll(duplicates);
            log.warn("Cleaned {} duplicate announcements for sourcePrimary={} sourceNoticeId={} canonicalId={}",
                    duplicates.size(), sourcePrimary, sourceNoticeId, canonical.getId());
        }
        return canonical;
    }

    SourcePairDuplicatePreflight preflightSourcePairDuplicates(SourceType sourcePrimary, String sourceNoticeId) {
        List<Announcement> candidates = announcementRepository.findSourcePairCandidates(sourcePrimary, sourceNoticeId);
        if (candidates.isEmpty()) {
            return new SourcePairDuplicatePreflight(sourcePrimary, sourceNoticeId, null, List.of());
        }
        Announcement canonical = candidates.get(0);
        List<Announcement> duplicates = candidates.size() == 1
                ? List.of()
                : List.copyOf(candidates.subList(1, candidates.size()));
        return new SourcePairDuplicatePreflight(sourcePrimary, sourceNoticeId, canonical, duplicates);
    }

    private void replaceUnits(Announcement announcement, String panId, JsonNode response, PdfParseResult pdfResult) {
        announcementUnitRepository.deleteByAnnouncementId(announcement.getId());

        List<LhUnitCandidate> lhCandidates = lhUnitCandidateExtractor.extract(
                panId,
                response,
                announcement.getSupplyTypeRaw(),
                announcement.getHouseTypeRaw(),
                announcement.getRegionLevel1());
        List<PdfParseResult.UnitItem> pdfUnits = pdfResult != null && pdfResult.units() != null
                ? pdfResult.units()
                : List.of();
        boolean[] matchedPdf = new boolean[pdfUnits.size()];

        for (int i = 0; i < lhCandidates.size(); i++) {
            LhUnitCandidate candidate = lhCandidates.get(i);
            int pdfIndex = matchingPdfIndex(candidate, pdfUnits, matchedPdf, i, lhCandidates.size() == pdfUnits.size());
            PdfParseResult.UnitItem pdfUnit = pdfIndex >= 0 ? pdfUnits.get(pdfIndex) : null;
            if (pdfIndex >= 0) matchedPdf[pdfIndex] = true;
            announcementUnitRepository.save(toAnnouncementUnit(announcement, candidate, pdfUnit));
        }

        for (int i = 0; i < pdfUnits.size(); i++) {
            if (!matchedPdf[i]) {
                announcementUnitRepository.save(toPdfAnnouncementUnit(announcement, pdfUnits.get(i), lhCandidates.size() + i));
            }
        }
    }

    private int matchingPdfIndex(LhUnitCandidate candidate,
                                 List<PdfParseResult.UnitItem> pdfUnits,
                                 boolean[] matchedPdf,
                                 int rowOrder,
                                 boolean sameCount) {
        for (int i = 0; i < pdfUnits.size(); i++) {
            if (!matchedPdf[i] && matches(candidate, pdfUnits.get(i))) {
                return i;
            }
        }
        if (sameCount && rowOrder < pdfUnits.size() && !matchedPdf[rowOrder]) {
            return rowOrder;
        }
        return -1;
    }

    private boolean matches(LhUnitCandidate candidate, PdfParseResult.UnitItem unit) {
        boolean addressMatch = candidate.fullAddress() != null && unit.address() != null
                && candidate.fullAddress().equals(unit.address());
        boolean complexMatch = candidate.complexName() != null && unit.complexName() != null
                && candidate.complexName().equals(unit.complexName());
        boolean areaMatch = candidate.exclusiveAreaText() != null && unit.exclusiveAreaText() != null
                && candidate.exclusiveAreaText().replace("㎡", "").trim().equals(unit.exclusiveAreaText().replace("㎡", "").trim());
        return (addressMatch || complexMatch) && (areaMatch || unit.exclusiveAreaText() == null);
    }

    private AnnouncementUnit toAnnouncementUnit(Announcement announcement,
                                                LhUnitCandidate candidate,
                                                PdfParseResult.UnitItem pdfUnit) {
        String houseTypeRaw = firstNonBlank(value(pdfUnit, PdfParseResult.UnitItem::houseType), candidate.houseTypeRaw());
        return AnnouncementUnit.builder()
                .announcement(announcement)
                .unitSource(pdfUnit == null ? AnnouncementUnitSource.LH_API : AnnouncementUnitSource.MERGED)
                .sourceUnitKey(candidate.sourceUnitKey())
                .unitOrder(candidate.unitOrder())
                .complexName(firstNonBlank(value(pdfUnit, PdfParseResult.UnitItem::complexName), candidate.complexName()))
                .fullAddress(firstNonBlank(value(pdfUnit, PdfParseResult.UnitItem::address), candidate.fullAddress()))
                .regionLevel1(firstNonBlank(value(pdfUnit, PdfParseResult.UnitItem::regionLevel1), candidate.regionLevel1()))
                .regionLevel2(firstNonBlank(value(pdfUnit, PdfParseResult.UnitItem::regionLevel2), candidate.regionLevel2()))
                .supplyTypeRaw(firstNonBlank(value(pdfUnit, PdfParseResult.UnitItem::supplyType), candidate.supplyTypeRaw()))
                .supplyTypeNormalized(normalizer.normalizeSupplyType(firstNonBlank(value(pdfUnit, PdfParseResult.UnitItem::supplyType), candidate.supplyTypeRaw())))
                .houseTypeRaw(houseTypeRaw)
                .houseTypeNormalized(normalizer.normalizeHouseType(houseTypeRaw))
                .exclusiveAreaText(firstNonBlank(value(pdfUnit, PdfParseResult.UnitItem::exclusiveAreaText), candidate.exclusiveAreaText()))
                .exclusiveAreaValue(pdfUnit != null && pdfUnit.exclusiveAreaValue() != null ? BigDecimal.valueOf(pdfUnit.exclusiveAreaValue()) : candidate.exclusiveAreaValue())
                .depositAmount(pdfUnit != null ? pdfUnit.depositAmountManwon() : null)
                .monthlyRentAmount(pdfUnit != null ? pdfUnit.monthlyRentAmountManwon() : null)
                .salePriceMin(pdfUnit != null ? pdfUnit.salePriceMinManwon() : null)
                .salePriceMax(pdfUnit != null ? pdfUnit.salePriceMaxManwon() : null)
                .salePriceRaw(pdfUnit != null ? pdfUnit.salePriceRaw() : null)
                .supplyHouseholdCount(pdfUnit != null && pdfUnit.supplyHouseholdCount() != null ? pdfUnit.supplyHouseholdCount() : candidate.supplyHouseholdCount())
                .rawText(pdfUnit != null && pdfUnit.rawText() != null ? pdfUnit.rawText() : candidate.rawText())
                .matchSource(pdfUnit == null ? MatchSource.RULE : MatchSource.AI)
                .confidenceLevel(pdfUnit == null ? ConfidenceLevel.HIGH : confidence(pdfUnit.confidence()))
                .build();
    }

    private AnnouncementUnit toPdfAnnouncementUnit(Announcement announcement, PdfParseResult.UnitItem unit, int order) {
        String supplyTypeRaw = unit.supplyType();
        String houseTypeRaw = unit.houseType();
        return AnnouncementUnit.builder()
                .announcement(announcement)
                .unitSource(AnnouncementUnitSource.PDF_AI)
                .sourceUnitKey("pdf-" + sha256(order + "|" + nullToEmpty(unit.rawText())).substring(0, 28))
                .unitOrder(order)
                .complexName(unit.complexName())
                .fullAddress(unit.address())
                .regionLevel1(unit.regionLevel1())
                .regionLevel2(unit.regionLevel2())
                .supplyTypeRaw(supplyTypeRaw)
                .supplyTypeNormalized(normalizer.normalizeSupplyType(supplyTypeRaw))
                .houseTypeRaw(houseTypeRaw)
                .houseTypeNormalized(normalizer.normalizeHouseType(houseTypeRaw))
                .exclusiveAreaText(unit.exclusiveAreaText())
                .exclusiveAreaValue(unit.exclusiveAreaValue() != null ? BigDecimal.valueOf(unit.exclusiveAreaValue()) : null)
                .depositAmount(unit.depositAmountManwon())
                .monthlyRentAmount(unit.monthlyRentAmountManwon())
                .salePriceMin(unit.salePriceMinManwon())
                .salePriceMax(unit.salePriceMaxManwon())
                .salePriceRaw(unit.salePriceRaw())
                .supplyHouseholdCount(unit.supplyHouseholdCount())
                .rawText(unit.rawText())
                .matchSource(MatchSource.AI)
                .confidenceLevel(confidence(unit.confidence()))
                .build();
    }

    private void saveEligibility(Announcement announcement, PdfParseResult pdfResult) {
        PdfParseResult.Eligibility e = pdfResult.eligibility();
        if (e == null) return;

        MaritalTargetType maritalTargetType = null;
        if (e.maritalTargetType() != null) {
            try {
                maritalTargetType = MaritalTargetType.valueOf(e.maritalTargetType());
            } catch (IllegalArgumentException ex) {
                log.warn("Unknown maritalTargetType from AI: {}", e.maritalTargetType());
            }
        }
        final MaritalTargetType resolvedMarital = maritalTargetType;

        announcementEligibilityRepository.findByAnnouncementId(announcement.getId())
                .ifPresentOrElse(
                        existing -> existing.update(
                                e.ageMin(), e.ageMax(), e.ageRawText(),
                                resolvedMarital, e.marriageYearLimit(), e.maritalRawText(),
                                e.childrenMinCount(), e.childrenRawText(),
                                e.homelessRequired(), e.homelessRawText(),
                                e.lowIncomeRequired(), e.incomeAssetCriteriaRaw(),
                                e.elderlyRequired(), e.elderlyAgeMin(), e.elderlyRawText(),
                                e.eligibilityRaw(), e.specialSupplyRaw()
                        ),
                        () -> announcementEligibilityRepository.save(
                                AnnouncementEligibility.builder()
                                        .announcement(announcement)
                                        .ageMin(e.ageMin()).ageMax(e.ageMax()).ageRawText(e.ageRawText())
                                        .maritalTargetType(resolvedMarital)
                                        .marriageYearLimit(e.marriageYearLimit()).maritalRawText(e.maritalRawText())
                                        .childrenMinCount(e.childrenMinCount()).childrenRawText(e.childrenRawText())
                                        .homelessRequired(e.homelessRequired()).homelessRawText(e.homelessRawText())
                                        .lowIncomeRequired(e.lowIncomeRequired()).incomeAssetCriteriaRaw(e.incomeAssetCriteriaRaw())
                                        .elderlyRequired(e.elderlyRequired()).elderlyAgeMin(e.elderlyAgeMin()).elderlyRawText(e.elderlyRawText())
                                        .eligibilityRaw(e.eligibilityRaw()).specialSupplyRaw(e.specialSupplyRaw())
                                        .build()
                        )
                );
    }

    private String value(PdfParseResult.UnitItem unit, Function<PdfParseResult.UnitItem, String> getter) {
        return unit == null ? null : getter.apply(unit);
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private ConfidenceLevel confidence(Double confidence) {
        if (confidence == null) return ConfidenceLevel.LOW;
        if (confidence >= 0.8) return ConfidenceLevel.HIGH;
        if (confidence >= 0.5) return ConfidenceLevel.MEDIUM;
        return ConfidenceLevel.LOW;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build unit key", e);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void saveParseRaw(Announcement announcement, String rawType, String rawText) {
        if (rawText == null) return;
        announcementParseRawRepository.deleteByAnnouncementIdAndRawType(announcement.getId(), rawType);
        announcementParseRawRepository.save(AnnouncementParseRaw.builder()
                .announcement(announcement)
                .rawType(rawType)
                .rawText(rawText)
                .collectedAt(LocalDateTime.now())
                .build());
    }

    private void saveCategories(Announcement announcement, String combinedText) {
        List<AnnouncementCategory> detected = categoryDetector.detect(announcement, combinedText);
        for (AnnouncementCategory category : detected) {
            if (!announcementCategoryRepository.existsByAnnouncementIdAndCategoryCode(
                    announcement.getId(), category.getCategoryCode())) {
                announcementCategoryRepository.save(category);
            }
        }
    }

    private String buildCombinedText(Announcement announcement, AnnouncementDetail detail, PdfParseResult pdfResult) {
        StringBuilder sb = new StringBuilder();
        append(sb, announcement.getNoticeName());
        append(sb, announcement.getSupplyTypeRaw());
        append(sb, announcement.getSupplyTypeNormalized());
        if (detail != null) {
            append(sb, detail.getGuideText());
            append(sb, detail.getEligibilityRaw());
            append(sb, detail.getIncomeAssetCriteriaRaw());
        }
        if (pdfResult != null) {
            append(sb, valueOf(pdfResult.incomeAssetCriteria()));
        }
        return sb.toString();
    }

    private String buildEligibilityText(PdfParseResult result) {
        return null;
    }

    private String serializeSchedules(List<PdfParseResult.ScheduleItem> items) {
        if (items == null || items.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize scheduleDetails", e);
            return null;
        }
    }

    private void append(StringBuilder sb, String text) {
        if (text != null && !text.isBlank()) {
            sb.append(" ").append(text);
        }
    }

    private String valueOf(PdfParseResult.Field field) {
        return field != null ? field.value() : null;
    }

    private String safeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void reconcileMergeGroup(Announcement saved) {
        String groupKey = saved.getMergedGroupKey();
        if (groupKey == null || groupKey.isBlank()) {
            saved.markActive();
            return;
        }
        List<Announcement> group = announcementRepository.findByMergedGroupKeyAndDeletedFalse(groupKey);
        if (group.isEmpty()) {
            saved.markActive();
            return;
        }
        Announcement canonical = group.stream()
                .min(Comparator.comparingInt((Announcement a) -> sourcePriority(a.getSourcePrimary()))
                        .thenComparing(Announcement::getId))
                .orElse(saved);
        for (Announcement a : group) {
            if (a.getId().equals(canonical.getId())) a.markActive();
            else a.markMerged();
        }
    }

    private int sourcePriority(SourceType sourceType) {
        return sourceType == SourceType.LH ? 0 : 1;
    }

    JsonNode findArray(JsonNode root, String fieldName) {
        if (root == null || !root.isArray()) return null;
        for (JsonNode node : root) {
            JsonNode candidate = node.get(fieldName);
            if (candidate != null && candidate.isArray()) return candidate;
        }
        return null;
    }

    JsonNode first(JsonNode arrayNode) {
        return arrayNode != null && arrayNode.isArray() && !arrayNode.isEmpty() ? arrayNode.get(0) : null;
    }

    String text(JsonNode node, String fieldName) {
        if (node == null || node.get(fieldName) == null || node.get(fieldName).isNull()) return null;
        String value = node.get(fieldName).asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Integer intValue(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        if (value == null) return null;
        try {
            return Integer.parseInt(value.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseSingleArea(String text) {
        if (text == null) return null;
        String[] matches = text.replaceAll("[^0-9.]+", " ").trim().split("\\s+");
        List<String> numbers = new ArrayList<>();
        for (String match : matches) {
            if (!match.isBlank()) numbers.add(match);
        }
        if (numbers.size() != 1) return null;
        try {
            return new BigDecimal(numbers.get(0));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
