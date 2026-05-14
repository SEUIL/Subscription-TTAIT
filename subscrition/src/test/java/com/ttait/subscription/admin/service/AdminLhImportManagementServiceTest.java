package com.ttait.subscription.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttait.subscription.admin.dto.LhCandidateCollectionResponse;
import com.ttait.subscription.admin.dto.LhImportRunResult;
import com.ttait.subscription.admin.dto.LhSelectedImportRequest;
import com.ttait.subscription.external.lh.LhApiClient;
import com.ttait.subscription.external.lh.LhImportCandidate;
import com.ttait.subscription.external.lh.LhImportCandidateRepository;
import com.ttait.subscription.external.lh.LhImportCandidateStatus;
import com.ttait.subscription.external.service.LhImportDecisionType;
import com.ttait.subscription.external.service.LhImportDedupeDecision;
import com.ttait.subscription.external.service.NoticeImportOrchestrator;
import com.ttait.subscription.external.support.CanonicalJsonHasher;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminLhImportManagementServiceTest {

    @Mock
    private LhApiClient lhApiClient;
    @Mock
    private LhImportCandidateRepository candidateRepository;
    @Mock
    private NoticeImportOrchestrator orchestrator;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CanonicalJsonHasher canonicalJsonHasher = new CanonicalJsonHasher(objectMapper);
    private AdminLhImportManagementService service;

    @BeforeEach
    void setUp() {
        service = new AdminLhImportManagementService(
                lhApiClient, candidateRepository, orchestrator, objectMapper, canonicalJsonHasher);
    }

    @Test
    void collectCandidatesStoresListAndDetailWithoutRunningImportPipeline() throws Exception {
        JsonNode item = item("PAN-001", "02");
        JsonNode detail = detail("https://example.com/pan-001.pdf");
        given(lhApiClient.fetchNoticeList(1, 1)).willReturn(list(item));
        given(lhApiClient.fetchNoticeDetail("PAN-001", "03", "050")).willReturn(detail);
        given(orchestrator.scanLhCandidate(item, detail, false)).willReturn(scan(
                LhImportDecisionType.NEW, null, "https://example.com/pan-001.pdf", true));
        given(candidateRepository.findByPanId("PAN-001")).willReturn(Optional.empty());
        given(candidateRepository.save(any(LhImportCandidate.class))).willAnswer(invocation -> {
            LhImportCandidate candidate = invocation.getArgument(0);
            assertThat(candidate.getCcrCnntSysDsCd()).isEqualTo("03");
            assertThat(candidate.getSplInfTpCd()).isEqualTo("050");
            assertThat(candidate.getRegionLevel1()).isEqualTo("Seoul");
            assertThat(candidate.getRegion()).isEqualTo("Seoul");
            assertThat(candidate.getNoticeStatusRaw()).isEqualTo("OPEN_RAW");
            assertThat(candidate.getListRawJson()).isEqualTo(objectMapper.writeValueAsString(item));
            assertThat(candidate.getItemJson()).isEqualTo(objectMapper.writeValueAsString(item));
            assertThat(candidate.getDetailRawJson()).isEqualTo(objectMapper.writeValueAsString(detail));
            assertThat(candidate.getDetailJson()).isEqualTo(objectMapper.writeValueAsString(detail));
            assertThat(candidate.getListHash()).isEqualTo(canonicalJsonHasher.hash(item));
            assertThat(candidate.getDetailHash()).isEqualTo(canonicalJsonHasher.hash(detail));
            assertThat(candidate.getCollectedAt()).isNotNull();
            ReflectionTestUtils.setField(candidate, "id", 10L);
            return candidate;
        });

        LhCandidateCollectionResponse response = service.collectCandidates(1, 1);

        assertThat(response.fetched()).isEqualTo(1);
        assertThat(response.scanned()).isEqualTo(1);
        assertThat(response.skippedLand()).isZero();
        assertThat(response.candidates()).hasSize(1);
        assertThat(response.candidates().get(0).panId()).isEqualTo("PAN-001");
        assertThat(response.candidates().get(0).region()).isEqualTo("Seoul");
        assertThat(response.candidates().get(0).canParse()).isTrue();
        then(orchestrator).should().scanLhCandidate(item, detail, false);
        then(orchestrator).shouldHaveNoMoreInteractions();
    }

    @Test
    void collectCandidatesSkipsLandDetailFetchAndStoresSkippedCandidate() throws Exception {
        JsonNode item = item("PAN-LAND", "01");
        given(lhApiClient.fetchNoticeList(1, 1)).willReturn(list(item));
        given(orchestrator.scanLhCandidate(item, null, false)).willReturn(scan(
                LhImportDecisionType.LAND_SKIP, null, null, false));
        given(candidateRepository.findByPanId("PAN-LAND")).willReturn(Optional.empty());
        given(candidateRepository.save(any(LhImportCandidate.class))).willAnswer(invocation -> {
            LhImportCandidate candidate = invocation.getArgument(0);
            assertThat(candidate.getListHash()).isEqualTo(canonicalJsonHasher.hash(item));
            assertThat(candidate.getDetailHash()).isNull();
            assertThat(candidate.getDetailRawJson()).isNull();
            ReflectionTestUtils.setField(candidate, "id", 11L);
            return candidate;
        });

        LhCandidateCollectionResponse response = service.collectCandidates(1, 1);

        assertThat(response.skippedLand()).isEqualTo(1);
        assertThat(response.candidates().get(0).status()).isEqualTo(LhImportCandidateStatus.SKIPPED.name());
        assertThat(response.candidates().get(0).isLandNotice()).isTrue();
        then(lhApiClient).should().fetchNoticeList(1, 1);
        then(lhApiClient).shouldHaveNoMoreInteractions();
    }

    @Test
    void importSelectedImportsOnlyRequestedCandidateIds() throws Exception {
        LhImportCandidate first = candidate(2L, "PAN-002");
        LhImportCandidate second = candidate(4L, "PAN-004");
        given(candidateRepository.findByIdIn(List.of(2L, 4L))).willReturn(List.of(first, second));
        given(orchestrator.importPreparedLhNotices(any(), any(Boolean.class))).willReturn(
                new NoticeImportOrchestrator.ImportResult(1, 0, 1, 1, 0, 0, 0, 0, false));

        LhImportRunResult result = service.importSelected(new LhSelectedImportRequest(List.of(2L, 4L), null));

        assertThat(result.imported()).isEqualTo(2);
        assertThat(first.getStatus()).isEqualTo(LhImportCandidateStatus.IMPORTED);
        assertThat(second.getStatus()).isEqualTo(LhImportCandidateStatus.IMPORTED);
        then(candidateRepository).should().findByIdIn(List.of(2L, 4L));
        then(orchestrator).should(times(2)).importPreparedLhNotices(any(), any(Boolean.class));
    }

    @Test
    void forceReparseUsesSharedOrchestratorForcePath() {
        LhImportRunResult result = service.forceReparse(99L);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.reparsed()).isEqualTo(1);
        then(orchestrator).should().reimportAnnouncement(99L);
    }

    private JsonNode list(JsonNode item) throws Exception {
        return objectMapper.readTree("""
                [{"dsList":[%s]}]
                """.formatted(objectMapper.writeValueAsString(item)));
    }

    private JsonNode item(String panId, String upperType) throws Exception {
        return objectMapper.readTree("""
                {
                  "PAN_ID":"%s",
                  "UPP_AIS_TP_CD":"%s",
                  "PAN_NM":"Test notice %s",
                  "CNP_CD_NM":"Seoul",
                  "DTL_URL":"https://example.com/%s",
                  "PAN_SS":"OPEN_RAW",
                  "CCR_CNNT_SYS_DS_CD":"03",
                  "SPL_INF_TP_CD":"050"
                }
                """.formatted(panId, upperType, panId, panId));
    }

    private JsonNode detail(String pdfUrl) throws Exception {
        return objectMapper.readTree("""
                [{"dsAhflInfo":[{"AHFL_URL":"%s","CMN_AHFL_NM":"notice.pdf"}]}]
                """.formatted(pdfUrl));
    }

    private NoticeImportOrchestrator.CandidateScanResult scan(LhImportDecisionType type,
                                                              Long announcementId,
                                                              String pdfUrl,
                                                              boolean shouldParseGemini) {
        return new NoticeImportOrchestrator.CandidateScanResult(
                new LhImportDedupeDecision(type, type.name(), true, shouldParseGemini, true,
                        false, announcementId, "PAN", "item", "detail", pdfUrl),
                pdfUrl
        );
    }

    private LhImportCandidate candidate(Long id, String panId) throws Exception {
        JsonNode item = item(panId, "02");
        JsonNode detail = detail("https://example.com/notice.pdf");
        LhImportCandidate candidate = new LhImportCandidate(panId);
        candidate.updateCollected("03", "050", "Test notice", "Seoul", "OPEN_RAW", "https://example.com/notice",
                "https://example.com/notice.pdf", false, false, true, "NEW",
                objectMapper.writeValueAsString(item), objectMapper.writeValueAsString(detail),
                canonicalJsonHasher.hash(item), canonicalJsonHasher.hash(detail));
        ReflectionTestUtils.setField(candidate, "id", id);
        return candidate;
    }
}
