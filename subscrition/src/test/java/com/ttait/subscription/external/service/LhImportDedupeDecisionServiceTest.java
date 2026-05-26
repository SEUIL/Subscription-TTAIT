package com.ttait.subscription.external.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementImportFingerprint;
import com.ttait.subscription.announcement.domain.AnnouncementImportParseStatus;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.domain.SourceType;
import com.ttait.subscription.announcement.repository.AnnouncementImportFingerprintRepository;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.external.support.CanonicalJsonHasher;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LhImportDedupeDecisionServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;
    @Mock
    private AnnouncementImportFingerprintRepository fingerprintRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CanonicalJsonHasher hasher = new CanonicalJsonHasher(objectMapper);
    private LhImportDedupeDecisionService service;

    @BeforeEach
    void setUp() {
        service = new LhImportDedupeDecisionService(announcementRepository, fingerprintRepository, hasher);
    }

    @Test
    void returnsLandSkipForLandNotice() throws Exception {
        LhImportDedupeDecision decision = service.decide(
                item("PAN-001", "01", "국민임대"), null, null, false);

        assertThat(decision.decision()).isEqualTo(LhImportDecisionType.LAND_SKIP);
        assertThat(decision.shouldFetchDetail()).isFalse();
        assertThat(decision.shouldParseGemini()).isFalse();
        assertThat(decision.shouldPersistOfficial()).isFalse();
        assertThat(decision.preserveExistingParsedData()).isTrue();
    }

    @Test
    void returnsCommercialSkipForCommercialNoticeName() throws Exception {
        LhImportDedupeDecision decision = service.decide(
                item("PAN-SHOP", "22", "24", "임대상가(추첨)"), null, null, false);

        assertThat(decision.decision()).isEqualTo(LhImportDecisionType.COMMERCIAL_SKIP);
        assertThat(decision.shouldFetchDetail()).isFalse();
        assertThat(decision.shouldParseGemini()).isFalse();
        assertThat(decision.shouldPersistOfficial()).isFalse();
        assertThat(decision.preserveExistingParsedData()).isTrue();
    }

    @Test
    void returnsCommercialSkipForCommercialCodePair() throws Exception {
        LhImportDedupeDecision decision = service.decide(
                item("PAN-SHOP", "22", "24", "기타"), null, null, false);

        assertThat(decision.decision()).isEqualTo(LhImportDecisionType.COMMERCIAL_SKIP);
        assertThat(decision.shouldFetchDetail()).isFalse();
        assertThat(decision.shouldParseGemini()).isFalse();
        assertThat(decision.shouldPersistOfficial()).isFalse();
    }

    @Test
    void returnsNewWhenNoExistingFingerprintExists() throws Exception {
        JsonNode item = item("PAN-001", "02", "국민임대");
        JsonNode detail = detail("서울", 1);
        given(announcementRepository.findSourcePairCandidates(SourceType.LH, "PAN-001"))
                .willReturn(List.of());

        LhImportDedupeDecision decision = service.decide(item, detail, "https://example.com/a.pdf", false);

        assertThat(decision.decision()).isEqualTo(LhImportDecisionType.NEW);
        assertThat(decision.shouldFetchDetail()).isTrue();
        assertThat(decision.shouldParseGemini()).isTrue();
        assertThat(decision.shouldPersistOfficial()).isTrue();
        assertThat(decision.preserveExistingParsedData()).isFalse();
    }

    @Test
    void returnsUnchangedSkipGeminiWhenFingerprintsMatchAndParseSucceeded() throws Exception {
        JsonNode item = item("PAN-001", "02", "국민임대");
        JsonNode detail = detail("서울", 1);
        givenExistingFingerprint(item, detail, "https://example.com/a.pdf", AnnouncementImportParseStatus.PARSED);

        LhImportDedupeDecision decision = service.decide(item, detail, "https://example.com/a.pdf", false);

        assertThat(decision.decision()).isEqualTo(LhImportDecisionType.UNCHANGED_SKIP_GEMINI);
        assertThat(decision.shouldFetchDetail()).isTrue();
        assertThat(decision.shouldParseGemini()).isFalse();
        assertThat(decision.shouldPersistOfficial()).isFalse();
        assertThat(decision.preserveExistingParsedData()).isTrue();
    }

    @Test
    void returnsChangedReparseWhenDetailFingerprintChanges() throws Exception {
        JsonNode item = item("PAN-001", "02", "국민임대");
        givenExistingFingerprint(item, detail("서울", 1), "https://example.com/a.pdf", AnnouncementImportParseStatus.PARSED);

        LhImportDedupeDecision decision = service.decide(item, detail("서울", 2), "https://example.com/a.pdf", false);

        assertThat(decision.decision()).isEqualTo(LhImportDecisionType.CHANGED_REPARSE);
        assertThat(decision.reason()).contains("detail");
        assertThat(decision.shouldParseGemini()).isTrue();
        assertThat(decision.shouldPersistOfficial()).isTrue();
    }

    @Test
    void returnsChangedReparseWhenPdfUrlChanges() throws Exception {
        JsonNode item = item("PAN-001", "02", "국민임대");
        JsonNode detail = detail("서울", 1);
        givenExistingFingerprint(item, detail, "https://example.com/a.pdf", AnnouncementImportParseStatus.PARSED);

        LhImportDedupeDecision decision = service.decide(item, detail, "https://example.com/b.pdf", false);

        assertThat(decision.decision()).isEqualTo(LhImportDecisionType.CHANGED_REPARSE);
        assertThat(decision.reason()).contains("PDF URL");
        assertThat(decision.shouldParseGemini()).isTrue();
        assertThat(decision.shouldPersistOfficial()).isTrue();
    }

    @Test
    void returnsFailedRetryWhenPreviousParseFailedEvenIfFingerprintsMatch() throws Exception {
        JsonNode item = item("PAN-001", "02", "국민임대");
        JsonNode detail = detail("서울", 1);
        givenExistingFingerprint(item, detail, "https://example.com/a.pdf", AnnouncementImportParseStatus.FAILED);

        LhImportDedupeDecision decision = service.decide(item, detail, "https://example.com/a.pdf", false);

        assertThat(decision.decision()).isEqualTo(LhImportDecisionType.FAILED_RETRY);
        assertThat(decision.shouldParseGemini()).isTrue();
        assertThat(decision.shouldPersistOfficial()).isTrue();
        assertThat(decision.preserveExistingParsedData()).isFalse();
    }

    @Test
    void returnsForceReparseWhenForceOverridesUnchangedFingerprints() throws Exception {
        JsonNode item = item("PAN-001", "02", "국민임대");
        JsonNode detail = detail("서울", 1);
        givenExistingFingerprint(item, detail, "https://example.com/a.pdf", AnnouncementImportParseStatus.PARSED);

        LhImportDedupeDecision decision = service.decide(item, detail, "https://example.com/a.pdf", true);

        assertThat(decision.decision()).isEqualTo(LhImportDecisionType.FORCE_REPARSE);
        assertThat(decision.shouldParseGemini()).isTrue();
        assertThat(decision.shouldPersistOfficial()).isTrue();
        assertThat(decision.preserveExistingParsedData()).isFalse();
    }

    @Test
    void returnsNoPdfWhenDetailHasNoPdfAttachment() throws Exception {
        JsonNode item = item("PAN-001", "02", "국민임대");
        JsonNode detail = detail("서울", 1);
        given(announcementRepository.findSourcePairCandidates(SourceType.LH, "PAN-001"))
                .willReturn(List.of());

        LhImportDedupeDecision decision = service.decide(item, detail, null, false);

        assertThat(decision.decision()).isEqualTo(LhImportDecisionType.NO_PDF);
        assertThat(decision.shouldFetchDetail()).isTrue();
        assertThat(decision.shouldParseGemini()).isFalse();
        assertThat(decision.shouldPersistOfficial()).isTrue();
    }

    private void givenExistingFingerprint(JsonNode item,
                                          JsonNode detail,
                                          String pdfUrl,
                                          AnnouncementImportParseStatus parseStatus) {
        Announcement announcement = announcement("PAN-001");
        AnnouncementImportFingerprint fingerprint = AnnouncementImportFingerprint.builder()
                .announcement(announcement)
                .lhItemHash(hasher.hash(item))
                .lhDetailHash(hasher.hash(detail))
                .pdfUrl(pdfUrl)
                .pdfAiJsonHash("pdf-ai-json-hash")
                .parseStatus(parseStatus)
                .lastCheckedAt(LocalDateTime.of(2026, 5, 13, 10, 0))
                .lastParsedAt(LocalDateTime.of(2026, 5, 13, 10, 1))
                .build();
        given(announcementRepository.findSourcePairCandidates(SourceType.LH, "PAN-001"))
                .willReturn(List.of(announcement));
        given(fingerprintRepository.findByAnnouncementId(1L)).willReturn(Optional.of(fingerprint));
    }

    private JsonNode item(String panId, String upperSupplyTypeCode, String name) throws Exception {
        return item(panId, upperSupplyTypeCode, "02", name);
    }

    private JsonNode item(String panId, String upperSupplyTypeCode, String supplyTypeCode, String name)
            throws Exception {
        return objectMapper.readTree("""
                {
                  "PAN_ID":"%s",
                  "UPP_AIS_TP_CD":"%s",
                  "AIS_TP_CD":"%s",
                  "AIS_TP_CD_NM":"%s",
                  "PAN_NM":"%s",
                  "CCR_CNNT_SYS_DS_CD":"03",
                  "SPL_INF_TP_CD":"050"
                }
                """.formatted(panId, upperSupplyTypeCode, supplyTypeCode, name, name));
    }

    private JsonNode detail(String region, int householdCount) throws Exception {
        return objectMapper.readTree("""
                [
                  {"dsSbd":[{"LGDN_ADR":"%s","HSH_CNT":"%d"}]},
                  {"dsAhflInfo":[{"AHFL_URL":"https://example.com/a.pdf","CMN_AHFL_NM":"notice.pdf"}]}
                ]
                """.formatted(region, householdCount));
    }

    private Announcement announcement(String panId) {
        Announcement announcement = Announcement.builder()
                .sourcePrimary(SourceType.LH)
                .sourceNoticeId(panId)
                .noticeName("국민임대")
                .providerName("LH")
                .sourceNoticeUrl("https://example.com/detail")
                .noticeStatus(AnnouncementStatus.OPEN)
                .collectedAt(LocalDateTime.of(2026, 5, 13, 10, 0))
                .build();
        ReflectionTestUtils.setField(announcement, "id", 1L);
        return announcement;
    }
}
