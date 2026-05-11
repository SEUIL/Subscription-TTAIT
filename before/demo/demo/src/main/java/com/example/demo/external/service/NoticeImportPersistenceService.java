package com.example.demo.external.service;

import com.example.demo.announcement.domain.Announcement;
import com.example.demo.announcement.domain.AnnouncementAttachment;
import com.example.demo.announcement.domain.AttachmentType;
import com.example.demo.announcement.domain.AnnouncementDetail;
import com.example.demo.announcement.domain.RawPayload;
import com.example.demo.announcement.domain.SourceType;
import com.example.demo.announcement.repository.AnnouncementAttachmentRepository;
import com.example.demo.announcement.repository.AnnouncementDetailRepository;
import com.example.demo.announcement.repository.AnnouncementRepository;
import com.example.demo.announcement.repository.RawPayloadRepository;
import com.example.demo.external.myhome.dto.MyHomeNoticeApiResponse;
import com.example.demo.external.support.AnnouncementNormalizer;
import com.example.demo.external.support.DateParsers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoticeImportPersistenceService {

    private final RawPayloadRepository rawPayloadRepository;
    private final AnnouncementRepository announcementRepository;
    private final AnnouncementAttachmentRepository announcementAttachmentRepository;
    private final AnnouncementDetailRepository announcementDetailRepository;
    private final AnnouncementNormalizer announcementNormalizer;
    private final ObjectMapper objectMapper;

    public NoticeImportPersistenceService(RawPayloadRepository rawPayloadRepository,
                                          AnnouncementRepository announcementRepository,
                                          AnnouncementAttachmentRepository announcementAttachmentRepository,
                                          AnnouncementDetailRepository announcementDetailRepository,
                                          AnnouncementNormalizer announcementNormalizer,
                                          ObjectMapper objectMapper) {
        this.rawPayloadRepository = rawPayloadRepository;
        this.announcementRepository = announcementRepository;
        this.announcementAttachmentRepository = announcementAttachmentRepository;
        this.announcementDetailRepository = announcementDetailRepository;
        this.announcementNormalizer = announcementNormalizer;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveRaw(SourceType sourceType, String apiName, String sourceNoticeId, Object payload) {
        try {
            rawPayloadRepository.save(RawPayload.builder()
                    .sourceType(sourceType)
                    .apiName(apiName)
                    .sourceNoticeId(sourceNoticeId)
                    .requestKey(null)
                    .payloadFormat("JSON")
                    .payloadText(objectMapper.writeValueAsString(payload))
                    .collectedAt(LocalDateTime.now())
                    .build());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize raw payload", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsertMyHome(MyHomeNoticeApiResponse.Item item) {
        String panId = announcementNormalizer.extractPanId(item.url());
        LocalDate announcementDate = DateParsers.parseDate(item.rcritPblancDe());
        LocalDate startDate = DateParsers.parseDate(item.beginDe());
        LocalDate endDate = DateParsers.parseDate(item.endDe());
        String providerName = announcementNormalizer.normalizeProviderName(item.suplyInsttNm(), "기타");
        String matchKey = announcementNormalizer.buildMatchKey(panId, "MYHOME", item.pblancNm(), startDate, endDate);
        String mergedGroupKey = panId != null ? "LH:" + panId : null;

        Announcement announcement = announcementRepository
                .findBySourcePrimaryAndSourceNoticeId(SourceType.MYHOME, item.pblancId())
                .orElseGet(() -> Announcement.builder()
                        .sourcePrimary(SourceType.MYHOME)
                        .sourceNoticeId(item.pblancId())
                        .noticeName(item.pblancNm())
                        .providerName(providerName)
                        .sourceNoticeUrl(item.url())
                        .sourcePcUrl(item.pcUrl())
                        .sourceMobileUrl(item.mobileUrl())
                        .noticeStatusRaw(item.sttusNm())
                        .noticeStatus(announcementNormalizer.calculateStatus(startDate, endDate, item.sttusNm()))
                        .announcementDate(announcementDate)
                        .applicationStartDate(startDate)
                        .applicationEndDate(endDate)
                        .winnerAnnouncementDate(DateParsers.parseDate(item.przwnerPresnatnDe()))
                        .regionLevel1(item.brtcNm())
                        .regionLevel2(item.signguNm())
                        .fullAddress(item.fullAdres())
                        .legalCode(item.pnu())
                        .complexName(item.hsmpNm())
                        .providerComplexHouseholdCount(item.totHshldCo())
                        .supplyTypeRaw(item.suplyTyNm())
                        .supplyTypeNormalized(announcementNormalizer.normalizeSupplyType(item.suplyTyNm()))
                        .houseTypeRaw(item.houseTyNm())
                        .houseTypeNormalized(announcementNormalizer.normalizeHouseType(item.houseTyNm()))
                        .depositAmount(item.rentGtn())
                        .monthlyRentAmount(item.mtRntchrg())
                        .supplyHouseholdCount(item.sumSuplyCo())
                        .matchKey(matchKey)
                        .merged(false)
                        .mergedGroupKey(mergedGroupKey)
                        .collectedAt(LocalDateTime.now())
                        .build());

        announcement.updateFromImport(
                item.pblancNm(),
                providerName,
                item.url(),
                item.pcUrl(),
                item.mobileUrl(),
                item.sttusNm(),
                announcementNormalizer.calculateStatus(startDate, endDate, item.sttusNm()),
                announcementDate,
                startDate,
                endDate,
                DateParsers.parseDate(item.przwnerPresnatnDe()),
                item.brtcNm(),
                item.signguNm(),
                item.fullAdres(),
                item.pnu(),
                item.hsmpNm(),
                item.totHshldCo(),
                item.suplyTyNm(),
                announcementNormalizer.normalizeSupplyType(item.suplyTyNm()),
                item.houseTyNm(),
                announcementNormalizer.normalizeHouseType(item.houseTyNm()),
                item.rentGtn(),
                item.mtRntchrg(),
                item.sumSuplyCo(),
                matchKey,
                false,
                mergedGroupKey,
                LocalDateTime.now()
        );
        announcement = announcementRepository.save(announcement);
        reconcileMergeGroup(announcement);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Announcement upsertLh(JsonNode item) {
        String panId = text(item, "PAN_ID");
        LocalDate announcementDate = DateParsers.parseDate(text(item, "PAN_NT_ST_DT"));
        LocalDate endDate = DateParsers.parseDate(text(item, "CLSG_DT"));
        String noticeName = text(item, "PAN_NM");
        String matchKey = announcementNormalizer.buildMatchKey(panId, "LH", noticeName, null, endDate);
        String mergedGroupKey = panId == null ? null : "LH:" + panId;

        Announcement announcement = announcementRepository
                .findBySourcePrimaryAndSourceNoticeId(SourceType.LH, panId)
                .orElseGet(() -> Announcement.builder()
                        .sourcePrimary(SourceType.LH)
                        .sourceNoticeId(panId)
                        .noticeName(noticeName)
                        .providerName("LH")
                        .sourceNoticeUrl(text(item, "DTL_URL"))
                        .sourcePcUrl(null)
                        .sourceMobileUrl(text(item, "DTL_URL_MOB"))
                        .noticeStatusRaw(text(item, "PAN_SS"))
                        .noticeStatus(announcementNormalizer.calculateStatus(null, endDate, text(item, "PAN_SS")))
                        .announcementDate(announcementDate)
                        .applicationStartDate(null)
                        .applicationEndDate(endDate)
                        .winnerAnnouncementDate(null)
                        .regionLevel1(text(item, "CNP_CD_NM"))
                        .regionLevel2(null)
                        .fullAddress(null)
                        .legalCode(null)
                        .complexName(null)
                        .providerComplexHouseholdCount(null)
                        .supplyTypeRaw(text(item, "AIS_TP_CD_NM"))
                        .supplyTypeNormalized(announcementNormalizer.normalizeSupplyType(text(item, "AIS_TP_CD_NM")))
                        .houseTypeRaw(null)
                        .houseTypeNormalized(null)
                        .depositAmount(null)
                        .monthlyRentAmount(null)
                        .supplyHouseholdCount(null)
                        .matchKey(matchKey)
                        .merged(false)
                        .mergedGroupKey(mergedGroupKey)
                        .collectedAt(LocalDateTime.now())
                        .build());

        announcement.updateFromImport(
                noticeName,
                "LH",
                text(item, "DTL_URL"),
                announcement.getSourcePcUrl(),
                text(item, "DTL_URL_MOB"),
                text(item, "PAN_SS"),
                announcementNormalizer.calculateStatus(null, endDate, text(item, "PAN_SS")),
                announcementDate,
                announcement.getApplicationStartDate(),
                endDate,
                announcement.getWinnerAnnouncementDate(),
                text(item, "CNP_CD_NM"),
                announcement.getRegionLevel2(),
                announcement.getFullAddress(),
                announcement.getLegalCode(),
                announcement.getComplexName(),
                announcement.getProviderComplexHouseholdCount(),
                text(item, "AIS_TP_CD_NM"),
                announcementNormalizer.normalizeSupplyType(text(item, "AIS_TP_CD_NM")),
                announcement.getHouseTypeRaw(),
                announcement.getHouseTypeNormalized(),
                announcement.getDepositAmount(),
                announcement.getMonthlyRentAmount(),
                announcement.getSupplyHouseholdCount(),
                matchKey,
                false,
                mergedGroupKey,
                LocalDateTime.now()
        );
        announcement = announcementRepository.save(announcement);
        reconcileMergeGroup(announcement);
        return announcement;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsertLhDetail(String panId, JsonNode response) {
        announcementRepository.findBySourcePrimaryAndSourceNoticeId(SourceType.LH, panId)
                .ifPresent(announcement -> {
                    JsonNode schedule = first(findArray(response, "dsSplScdl"));
                    JsonNode site = first(findArray(response, "dsSbd"));
                    JsonNode etc = first(findArray(response, "dsEtcInfo"));
                    JsonNode contact = first(findArray(response, "dsCtrtPlc"));

                    AnnouncementDetail detail = announcementDetailRepository.findByAnnouncementIdAndDeletedFalse(announcement.getId())
                            .orElseGet(() -> AnnouncementDetail.builder()
                                    .announcement(announcement)
                                    .applicationDatetimeText(null)
                                    .documentSubmitStartDate(null)
                                    .documentSubmitEndDate(null)
                                    .contractStartDate(null)
                                    .contractEndDate(null)
                                    .complexName(null)
                                    .complexAddress(null)
                                    .complexDetailAddress(null)
                                    .householdCount(null)
                                    .heatingType(null)
                                    .exclusiveAreaText(null)
                                    .exclusiveAreaValue(null)
                                    .moveInExpectedYm(null)
                                    .guideText(null)
                                    .contactPhone(null)
                                    .contactAddress(null)
                                    .contactGuideText(null)
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
                    announcementDetailRepository.save(detail);
                    upsertAttachments(announcement, findArray(response, "dsAhflInfo"));
                });
    }

    JsonNode findArray(JsonNode root, String fieldName) {
        if (root == null || !root.isArray()) {
            return null;
        }
        for (JsonNode node : root) {
            JsonNode candidate = node.get(fieldName);
            if (candidate != null && candidate.isArray()) {
                return candidate;
            }
        }
        return null;
    }

    JsonNode first(JsonNode arrayNode) {
        return arrayNode != null && arrayNode.isArray() && !arrayNode.isEmpty() ? arrayNode.get(0) : null;
    }

    String text(JsonNode node, String fieldName) {
        if (node == null || node.get(fieldName) == null || node.get(fieldName).isNull()) {
            return null;
        }
        String value = node.get(fieldName).asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    Integer intValue(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    BigDecimal parseSingleArea(String text) {
        if (text == null) {
            return null;
        }
        String[] matches = text.replaceAll("[^0-9.]+", " ").trim().split("\\s+");
        List<String> numbers = new ArrayList<>();
        for (String match : matches) {
            if (!match.isBlank()) {
                numbers.add(match);
            }
        }
        if (numbers.size() != 1) {
            return null;
        }
        try {
            return new BigDecimal(numbers.get(0));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void reconcileMergeGroup(Announcement savedAnnouncement) {
        String mergedGroupKey = savedAnnouncement.getMergedGroupKey();
        if (mergedGroupKey == null || mergedGroupKey.isBlank()) {
            savedAnnouncement.markActive();
            return;
        }

        List<Announcement> groupAnnouncements = announcementRepository.findByMergedGroupKeyAndDeletedFalse(mergedGroupKey);
        if (groupAnnouncements.isEmpty()) {
            savedAnnouncement.markActive();
            return;
        }

        Announcement canonical = groupAnnouncements.stream()
                .min(Comparator
                        .comparingInt((Announcement announcement) -> sourcePriority(announcement.getSourcePrimary()))
                        .thenComparing(Announcement::getId))
                .orElse(savedAnnouncement);

        for (Announcement announcement : groupAnnouncements) {
            if (announcement.getId().equals(canonical.getId())) {
                announcement.markActive();
            } else {
                announcement.markMerged();
            }
        }
    }

    private int sourcePriority(SourceType sourceType) {
        return sourceType == SourceType.LH ? 0 : 1;
    }

    private void upsertAttachments(Announcement announcement, JsonNode attachmentArray) {
        if (attachmentArray == null || !attachmentArray.isArray()) {
            return;
        }
        for (JsonNode attachmentNode : attachmentArray) {
            String url = text(attachmentNode, "AHFL_URL");
            String name = text(attachmentNode, "CMN_AHFL_NM");
            if (url == null || name == null || "다운로드".equals(url)) {
                continue;
            }
            if (announcementAttachmentRepository.existsByAnnouncementIdAndAttachmentUrlAndDeletedFalse(announcement.getId(), url)) {
                continue;
            }
            announcementAttachmentRepository.save(AnnouncementAttachment.builder()
                    .announcement(announcement)
                    .attachmentType(resolveAttachmentType(text(attachmentNode, "SL_PAN_AHFL_DS_CD_NM"), name, url))
                    .attachmentName(name)
                    .attachmentUrl(url)
                    .build());
        }
    }

    private AttachmentType resolveAttachmentType(String rawType, String name, String url) {
        String value = ((rawType == null ? "" : rawType) + " " + (name == null ? "" : name) + " " + (url == null ? "" : url)).toLowerCase();
        if (value.contains("pdf")) {
            return AttachmentType.PDF;
        }
        if (value.contains("hwp")) {
            return AttachmentType.HWP;
        }
        if (value.contains("카탈로그") || value.contains("catalog")) {
            return AttachmentType.CATALOG;
        }
        if (value.contains(".jpg") || value.contains(".jpeg") || value.contains(".png") || value.contains("imageview")) {
            return AttachmentType.IMAGE;
        }
        return AttachmentType.OTHER;
    }
}
