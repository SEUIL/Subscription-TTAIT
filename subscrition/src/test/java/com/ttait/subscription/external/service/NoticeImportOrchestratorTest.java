package com.ttait.subscription.external.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementParseRaw;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.domain.SourceType;
import com.ttait.subscription.announcement.repository.AnnouncementParseRawRepository;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.external.ai.dto.PdfParseResult;
import com.ttait.subscription.external.lh.LhApiClient;
import com.ttait.subscription.external.pdf.PdfParsingService;
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
class NoticeImportOrchestratorTest {

    @Mock
    private LhApiClient lhApiClient;
    @Mock
    private NoticeImportPersistenceService persistenceService;
    @Mock
    private PdfParsingService pdfParsingService;
    @Mock
    private LhImportDedupeDecisionService dedupeDecisionService;
    @Mock
    private AnnouncementUnitGeocodingEnrichmentService geocodingEnrichmentService;
    @Mock
    private AnnouncementRepository announcementRepository;
    @Mock
    private AnnouncementParseRawRepository announcementParseRawRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private NoticeImportOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new NoticeImportOrchestrator(
                lhApiClient,
                persistenceService,
                pdfParsingService,
                dedupeDecisionService,
                geocodingEnrichmentService,
                objectMapper,
                announcementRepository,
                announcementParseRawRepository
        );
    }

    @Test
    void legacyImportMethodUsesForceFalseAndReturnsImportedFailedShape() throws Exception {
        ImportFixture fixture = givenSingleNoticePage(false, true);
        Announcement announcement = announcement();
        PdfParseResult pdfResult = pdfResult();
        LhImportDedupeDecision listDecision = decision(LhImportDecisionType.NEW, false, true, false);
        LhImportDedupeDecision changedDecision = decision(LhImportDecisionType.CHANGED_REPARSE, true, true, false);
        given(dedupeDecisionService.decide(fixture.item(), null, null, false)).willReturn(listDecision);
        given(dedupeDecisionService.decide(fixture.item(), fixture.detail(), fixture.pdfUrl(), false)).willReturn(changedDecision);
        given(persistenceService.upsertLh(fixture.item())).willReturn(announcement);
        given(pdfParsingService.parse(fixture.pdfUrl())).willReturn(pdfResult);

        NoticeImportOrchestrator.ImportResult result = orchestrator.importLhNotices(1, 1);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        then(pdfParsingService).should().parse(fixture.pdfUrl());
        then(persistenceService).should().upsertLhDetail(eq("PAN-001"), eq(fixture.detail()), eq(pdfResult), anyString());
        then(geocodingEnrichmentService).should().enrichNotRequestedUnits(1L);
        then(dedupeDecisionService).should().recordSuccess(announcement, changedDecision, objectMapper.writeValueAsString(pdfResult));
    }

    @Test
    void schedulerImportMethodUsesForceFalseAndReportsInternalCounters() throws Exception {
        ImportFixture fixture = givenSingleNoticePage(false, true);
        Announcement announcement = announcement();
        PdfParseResult pdfResult = pdfResult();
        LhImportDedupeDecision listDecision = decision(LhImportDecisionType.NEW, false, true, false);
        LhImportDedupeDecision changedDecision = decision(LhImportDecisionType.CHANGED_REPARSE, true, true, false);
        given(dedupeDecisionService.decide(fixture.item(), null, null, false)).willReturn(listDecision);
        given(dedupeDecisionService.decide(fixture.item(), fixture.detail(), fixture.pdfUrl(), false)).willReturn(changedDecision);
        given(persistenceService.upsertLh(fixture.item())).willReturn(announcement);
        given(pdfParsingService.parse(fixture.pdfUrl())).willReturn(pdfResult);

        NoticeImportOrchestrator.ImportResult result = orchestrator.importLhNoticesForScheduler(1, 1);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        assertThat(result.fetched()).isEqualTo(1);
        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.unchanged()).isZero();
        assertThat(result.geminiSkipped()).isZero();
        assertThat(result.endOfList()).isFalse();
        then(dedupeDecisionService).should().decide(fixture.item(), null, null, false);
        then(dedupeDecisionService).should().decide(fixture.item(), fixture.detail(), fixture.pdfUrl(), false);
    }

    @Test
    void unchangedNoticeShortCircuitsBeforePdfParsingAndDetailPersistence() throws Exception {
        ImportFixture fixture = givenSingleNoticePage(false, true);
        LhImportDedupeDecision listDecision = decision(LhImportDecisionType.NEW, false, true, false);
        LhImportDedupeDecision unchangedDecision = decision(LhImportDecisionType.UNCHANGED_SKIP_GEMINI, false, false, true);
        given(dedupeDecisionService.decide(fixture.item(), null, null, false)).willReturn(listDecision);
        given(dedupeDecisionService.decide(fixture.item(), fixture.detail(), fixture.pdfUrl(), false)).willReturn(unchangedDecision);

        NoticeImportOrchestrator.ImportResult result = orchestrator.importLhNotices(1, 1);

        assertThat(result.imported()).isZero();
        assertThat(result.failed()).isZero();
        assertThat(result.fetched()).isEqualTo(1);
        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.unchanged()).isEqualTo(1);
        assertThat(result.geminiSkipped()).isEqualTo(1);
        then(dedupeDecisionService).should().recordChecked(unchangedDecision);
        then(pdfParsingService).should(never()).parse(anyString());
        then(persistenceService).should(never()).upsertLh(any(JsonNode.class));
        then(persistenceService).should(never()).upsertLhDetail(anyString(), any(JsonNode.class), any(), any());
    }

    @Test
    void preserveParsedDataWithoutParsingDoesNotReplaceExistingUnitsWithNullPdfResult() throws Exception {
        ImportFixture fixture = givenSingleNoticePage(false, false);
        LhImportDedupeDecision listDecision = decision(LhImportDecisionType.NEW, false, true, false);
        LhImportDedupeDecision noPdfPreserveDecision = new LhImportDedupeDecision(
                LhImportDecisionType.NO_PDF,
                "LH detail response has no PDF attachment",
                true,
                false,
                true,
                true,
                1L,
                "PAN-001",
                "item-hash",
                "detail-hash",
                null
        );
        given(dedupeDecisionService.decide(fixture.item(), null, null, false)).willReturn(listDecision);
        given(dedupeDecisionService.decide(fixture.item(), fixture.detail(), null, false)).willReturn(noPdfPreserveDecision);

        NoticeImportOrchestrator.ImportResult result = orchestrator.importLhNotices(1, 1);

        assertThat(result.imported()).isZero();
        assertThat(result.failed()).isZero();
        assertThat(result.fetched()).isEqualTo(1);
        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.geminiSkipped()).isEqualTo(1);
        then(dedupeDecisionService).should().recordChecked(noPdfPreserveDecision);
        then(pdfParsingService).should(never()).parse(anyString());
        then(persistenceService).should(never()).upsertLhDetail(anyString(), any(JsonNode.class), any(), any());
    }

    @Test
    void forceAdminModeUsesForceTrueAndRunsPdfPersistencePath() throws Exception {
        ImportFixture fixture = givenSingleNoticePage(true, true);
        Announcement announcement = announcement();
        PdfParseResult pdfResult = pdfResult();
        LhImportDedupeDecision listDecision = decision(LhImportDecisionType.FORCE_REPARSE, true, true, false);
        LhImportDedupeDecision forceDecision = decision(LhImportDecisionType.FORCE_REPARSE, true, true, false);
        given(dedupeDecisionService.decide(fixture.item(), null, null, true)).willReturn(listDecision);
        given(dedupeDecisionService.decide(fixture.item(), fixture.detail(), fixture.pdfUrl(), true)).willReturn(forceDecision);
        given(persistenceService.upsertLh(fixture.item())).willReturn(announcement);
        given(pdfParsingService.parse(fixture.pdfUrl())).willReturn(pdfResult);

        NoticeImportOrchestrator.ImportResult result = orchestrator.importLhNotices(
                1, 1, NoticeImportOrchestrator.ImportMode.FORCE_ADMIN);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        then(pdfParsingService).should().parse(fixture.pdfUrl());
        then(persistenceService).should().upsertLhDetail(eq("PAN-001"), eq(fixture.detail()), eq(pdfResult), anyString());
        then(geocodingEnrichmentService).should().enrichNotRequestedUnits(1L);
        then(dedupeDecisionService).should().recordSuccess(announcement, forceDecision, objectMapper.writeValueAsString(pdfResult));
    }

    @Test
    void geocodingEnrichmentFailureDoesNotFailImportSuccess() throws Exception {
        ImportFixture fixture = givenSingleNoticePage(false, true);
        Announcement announcement = announcement();
        PdfParseResult pdfResult = pdfResult();
        LhImportDedupeDecision listDecision = decision(LhImportDecisionType.NEW, false, true, false);
        LhImportDedupeDecision changedDecision = decision(LhImportDecisionType.CHANGED_REPARSE, true, true, false);
        given(dedupeDecisionService.decide(fixture.item(), null, null, false)).willReturn(listDecision);
        given(dedupeDecisionService.decide(fixture.item(), fixture.detail(), fixture.pdfUrl(), false)).willReturn(changedDecision);
        given(persistenceService.upsertLh(fixture.item())).willReturn(announcement);
        given(pdfParsingService.parse(fixture.pdfUrl())).willReturn(pdfResult);
        willThrow(new RuntimeException("geocode down"))
                .given(geocodingEnrichmentService).enrichNotRequestedUnits(1L);

        NoticeImportOrchestrator.ImportResult result = orchestrator.importLhNotices(1, 1);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        then(persistenceService).should().upsertLhDetail(eq("PAN-001"), eq(fixture.detail()), eq(pdfResult), anyString());
        then(geocodingEnrichmentService).should().enrichNotRequestedUnits(1L);
        then(dedupeDecisionService).should().recordSuccess(announcement, changedDecision, objectMapper.writeValueAsString(pdfResult));
    }

    @Test
    void reimportAnnouncementRunsGeocodingAfterReplacingUnits() throws Exception {
        ReimportFixture fixture = givenReimportNotice();
        PdfParseResult pdfResult = pdfResult();
        LhImportDedupeDecision forceDecision = decision(LhImportDecisionType.FORCE_REPARSE, true, true, false);
        given(dedupeDecisionService.decide(fixture.item(), fixture.detail(), fixture.pdfUrl(), true)).willReturn(forceDecision);
        given(pdfParsingService.parse(fixture.pdfUrl())).willReturn(pdfResult);

        orchestrator.reimportAnnouncement(1L);

        then(persistenceService).should().upsertLhDetail(eq("PAN-001"), eq(fixture.detail()), eq(pdfResult), anyString());
        then(geocodingEnrichmentService).should().enrichNotRequestedUnits(1L);
        then(dedupeDecisionService).should()
                .recordSuccess(fixture.announcement(), forceDecision, objectMapper.writeValueAsString(pdfResult));
    }

    @Test
    void reimportAnnouncementIgnoresGeocodingFailureAndRecordsSuccess() throws Exception {
        ReimportFixture fixture = givenReimportNotice();
        PdfParseResult pdfResult = pdfResult();
        LhImportDedupeDecision forceDecision = decision(LhImportDecisionType.FORCE_REPARSE, true, true, false);
        given(dedupeDecisionService.decide(fixture.item(), fixture.detail(), fixture.pdfUrl(), true)).willReturn(forceDecision);
        given(pdfParsingService.parse(fixture.pdfUrl())).willReturn(pdfResult);
        willThrow(new RuntimeException("geocode down"))
                .given(geocodingEnrichmentService).enrichNotRequestedUnits(1L);

        orchestrator.reimportAnnouncement(1L);

        then(geocodingEnrichmentService).should().enrichNotRequestedUnits(1L);
        then(dedupeDecisionService).should()
                .recordSuccess(fixture.announcement(), forceDecision, objectMapper.writeValueAsString(pdfResult));
    }

    @Test
    void parseFailureRecordsFingerprintFailureSoNextNonForceRunCanRetry() throws Exception {
        ImportFixture fixture = givenSingleNoticePage(false, true);
        Announcement announcement = announcement();
        LhImportDedupeDecision listDecision = decision(LhImportDecisionType.NEW, false, true, false);
        LhImportDedupeDecision newDecision = decision(LhImportDecisionType.NEW, true, true, false);
        RuntimeException parseFailure = new RuntimeException("boom");
        given(dedupeDecisionService.decide(fixture.item(), null, null, false)).willReturn(listDecision);
        given(dedupeDecisionService.decide(fixture.item(), fixture.detail(), fixture.pdfUrl(), false)).willReturn(newDecision);
        given(persistenceService.upsertLh(fixture.item())).willReturn(announcement);
        given(pdfParsingService.parse(fixture.pdfUrl())).willThrow(parseFailure);

        NoticeImportOrchestrator.ImportResult result = orchestrator.importLhNotices(1, 1);

        assertThat(result.imported()).isZero();
        assertThat(result.failed()).isEqualTo(1);
        then(dedupeDecisionService).should().recordFailure(announcement, newDecision, "boom");
        then(persistenceService).should(never()).upsertLhDetail(anyString(), any(JsonNode.class), any(), any());
        then(dedupeDecisionService).should(never()).recordSuccess(any(), any(), any());
    }

    private ImportFixture givenSingleNoticePage(boolean force, boolean hasPdf) throws Exception {
        JsonNode listResponse = objectMapper.readTree("""
                {
                  "dsList": [
                    {
                      "PAN_ID":"PAN-001",
                      "UPP_AIS_TP_CD":"02",
                      "PAN_NM":"국민임대",
                      "CCR_CNNT_SYS_DS_CD":"03",
                      "SPL_INF_TP_CD":"050"
                    }
                  ]
                }
                """);
        JsonNode item = listResponse.get("dsList").get(0);
        JsonNode detail = hasPdf
                ? objectMapper.readTree("""
                        {"dsAhflInfo":[{"AHFL_URL":"https://example.com/a.pdf","CMN_AHFL_NM":"notice.pdf"}]}
                        """)
                : objectMapper.readTree("{" + "\"dsAhflInfo\":[]" + "}");
        String pdfUrl = hasPdf ? "https://example.com/a.pdf" : null;

        given(lhApiClient.fetchNoticeList(1, 1)).willReturn(listResponse);
        given(persistenceService.findArray(listResponse, "dsList")).willReturn(listResponse.get("dsList"));
        given(persistenceService.text(item, "PAN_ID")).willReturn("PAN-001");
        given(persistenceService.text(item, "CCR_CNNT_SYS_DS_CD")).willReturn("03");
        given(persistenceService.text(item, "SPL_INF_TP_CD")).willReturn("050");
        given(lhApiClient.fetchNoticeDetail("PAN-001", "03", "050")).willReturn(detail);
        given(persistenceService.findArray(detail, "dsAhflInfo")).willReturn(detail.get("dsAhflInfo"));
        if (hasPdf) {
            JsonNode attachment = detail.get("dsAhflInfo").get(0);
            given(persistenceService.text(attachment, "AHFL_URL")).willReturn(pdfUrl);
            given(persistenceService.text(attachment, "CMN_AHFL_NM")).willReturn("notice.pdf");
        }
        return new ImportFixture(item, detail, pdfUrl, force);
    }

    private ReimportFixture givenReimportNotice() throws Exception {
        Announcement announcement = announcement();
        JsonNode item = objectMapper.readTree("""
                {
                  "PAN_ID":"PAN-001",
                  "CCR_CNNT_SYS_DS_CD":"03",
                  "SPL_INF_TP_CD":"050"
                }
                """);
        JsonNode detail = objectMapper.readTree("""
                {"dsAhflInfo":[{"AHFL_URL":"https://example.com/a.pdf","CMN_AHFL_NM":"notice.pdf"}]}
                """);
        String pdfUrl = "https://example.com/a.pdf";
        AnnouncementParseRaw raw = AnnouncementParseRaw.builder()
                .announcement(announcement)
                .rawType("LH_ITEM_JSON")
                .rawText(objectMapper.writeValueAsString(item))
                .collectedAt(LocalDateTime.now())
                .build();

        given(announcementRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(announcement));
        given(announcementParseRawRepository.findByAnnouncementIdAndRawType(1L, "LH_ITEM_JSON")).willReturn(Optional.of(raw));
        given(persistenceService.text(item, "PAN_ID")).willReturn("PAN-001");
        given(persistenceService.text(item, "CCR_CNNT_SYS_DS_CD")).willReturn("03");
        given(persistenceService.text(item, "SPL_INF_TP_CD")).willReturn("050");
        given(lhApiClient.fetchNoticeDetail("PAN-001", "03", "050")).willReturn(detail);
        given(persistenceService.findArray(detail, "dsAhflInfo")).willReturn(detail.get("dsAhflInfo"));
        JsonNode attachment = detail.get("dsAhflInfo").get(0);
        given(persistenceService.text(attachment, "AHFL_URL")).willReturn(pdfUrl);
        given(persistenceService.text(attachment, "CMN_AHFL_NM")).willReturn("notice.pdf");
        return new ReimportFixture(announcement, item, detail, pdfUrl);
    }

    private LhImportDedupeDecision decision(LhImportDecisionType decisionType,
                                            boolean shouldParseGemini,
                                            boolean shouldPersistOfficial,
                                            boolean preserveExistingParsedData) {
        return new LhImportDedupeDecision(
                decisionType,
                decisionType.name(),
                true,
                shouldParseGemini,
                shouldPersistOfficial,
                preserveExistingParsedData,
                1L,
                "PAN-001",
                "item-hash",
                "detail-hash",
                "https://example.com/a.pdf"
        );
    }

    private PdfParseResult pdfResult() {
        return new PdfParseResult(
                "임대",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                "아파트",
                "경기도 성남시"
        );
    }

    private Announcement announcement() {
        Announcement announcement = Announcement.builder()
                .sourcePrimary(SourceType.LH)
                .sourceNoticeId("PAN-001")
                .noticeName("국민임대")
                .providerName("LH")
                .sourceNoticeUrl("https://example.com/detail")
                .noticeStatus(AnnouncementStatus.OPEN)
                .collectedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(announcement, "id", 1L);
        return announcement;
    }

    private record ImportFixture(JsonNode item, JsonNode detail, String pdfUrl, boolean force) {
    }

    private record ReimportFixture(Announcement announcement, JsonNode item, JsonNode detail, String pdfUrl) {
    }
}
