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
import com.ttait.subscription.announcement.domain.ConfidenceLevel;
import com.ttait.subscription.announcement.domain.MaritalTargetType;
import com.ttait.subscription.announcement.domain.SourceType;
import com.ttait.subscription.announcement.repository.AnnouncementCategoryRepository;
import com.ttait.subscription.announcement.repository.AnnouncementDetailRepository;
import com.ttait.subscription.announcement.repository.AnnouncementEligibilityRepository;
import com.ttait.subscription.announcement.repository.AnnouncementParseRawRepository;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.external.ai.dto.PdfParseResult;
import com.ttait.subscription.external.support.AnnouncementNormalizer;
import com.ttait.subscription.external.support.CategoryDetector;
import com.ttait.subscription.external.support.DateParsers;
import com.ttait.subscription.external.support.SupplyCountParser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    private final AnnouncementNormalizer normalizer;
    private final CategoryDetector categoryDetector;
    private final SupplyCountParser supplyCountParser;
    private final ObjectMapper objectMapper;

    public NoticeImportPersistenceService(AnnouncementRepository announcementRepository,
                                          AnnouncementDetailRepository announcementDetailRepository,
                                          AnnouncementCategoryRepository announcementCategoryRepository,
                                          AnnouncementParseRawRepository announcementParseRawRepository,
                                          AnnouncementEligibilityRepository announcementEligibilityRepository,
                                          AnnouncementNormalizer normalizer,
                                          CategoryDetector categoryDetector,
                                          SupplyCountParser supplyCountParser,
                                          ObjectMapper objectMapper) {
        this.announcementRepository = announcementRepository;
        this.announcementDetailRepository = announcementDetailRepository;
        this.announcementCategoryRepository = announcementCategoryRepository;
        this.announcementParseRawRepository = announcementParseRawRepository;
        this.announcementEligibilityRepository = announcementEligibilityRepository;
        this.normalizer = normalizer;
        this.categoryDetector = categoryDetector;
        this.supplyCountParser = supplyCountParser;
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

        Announcement announcement = announcementRepository
                .findBySourcePrimaryAndSourceNoticeId(SourceType.LH, panId)
                .orElseGet(() -> Announcement.builder()
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
                        .build());

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
        announcementRepository.findBySourcePrimaryAndSourceNoticeId(SourceType.LH, panId)
                .ifPresent(announcement -> {
                    JsonNode schedule = first(findArray(response, "dsSplScdl"));
                    JsonNode site = first(findArray(response, "dsSbd"));
                    JsonNode etc = first(findArray(response, "dsEtcInfo"));
                    JsonNode contact = first(findArray(response, "dsCtrtPlc"));

                    AnnouncementDetail detail = announcementDetailRepository
                            .findByAnnouncementIdAndDeletedFalse(announcement.getId())
                            .orElseGet(() -> AnnouncementDetail.builder()
                                    .announcement(announcement)
                                    .build());

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
                                buildEligibilityText(pdfResult)
                        );
                        detail.updateEligibilityRaw(eligibilityRawText);

                        // supply_household_count 업데이트 (HIGH 신뢰도일 때만)
                        if (supplyParsed.count() != null && supplyParsed.confidence() == ConfidenceLevel.HIGH) {
                            announcement.updateSupplyHouseholdCount(supplyParsed.count());
                        }
                        // deposit / monthly_rent 업데이트
                        announcement.updateDepositAndRent(pdfResult.depositAmountManwon(), pdfResult.monthlyRentAmountManwon());
                        announcementRepository.save(announcement);

                        // eligibility 저장
                        saveEligibility(announcement, pdfResult);
                    }

                    announcementDetailRepository.save(detail);

                    // raw 저장 (API JSON)
                    saveParseRaw(announcement, "API_JSON", safeJson(response));
                    // raw 저장 (PDF AI JSON)
                    if (pdfRawJson != null) {
                        saveParseRaw(announcement, "PDF_AI_JSON", pdfRawJson);
                    }

                    // 카테고리 감지
                    String combinedText = buildCombinedText(announcement, detail, pdfResult);
                    saveCategories(announcement, combinedText);
                });
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
        // TODO: announcement_eligibility 구현 시 OpenAI 프롬프트에 eligibility 필드 추가 후 채울 것
        return null;
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
