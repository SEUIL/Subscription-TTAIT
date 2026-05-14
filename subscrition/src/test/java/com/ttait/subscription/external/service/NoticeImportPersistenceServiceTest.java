package com.ttait.subscription.external.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.announcement.domain.AnnouncementUnitSource;
import com.ttait.subscription.announcement.domain.SourceType;
import com.ttait.subscription.announcement.repository.AnnouncementCategoryRepository;
import com.ttait.subscription.announcement.repository.AnnouncementDetailRepository;
import com.ttait.subscription.announcement.repository.AnnouncementEligibilityRepository;
import com.ttait.subscription.announcement.repository.AnnouncementParseRawRepository;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.announcement.repository.AnnouncementUnitRepository;
import com.ttait.subscription.external.lh.fixture.LhDetailResponseFixtures;
import com.ttait.subscription.external.support.AnnouncementNormalizer;
import com.ttait.subscription.external.support.CategoryDetector;
import com.ttait.subscription.external.support.SupplyCountParser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NoticeImportPersistenceServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;
    @Mock
    private AnnouncementDetailRepository announcementDetailRepository;
    @Mock
    private AnnouncementCategoryRepository announcementCategoryRepository;
    @Mock
    private AnnouncementParseRawRepository announcementParseRawRepository;
    @Mock
    private AnnouncementEligibilityRepository announcementEligibilityRepository;
    @Mock
    private AnnouncementUnitRepository announcementUnitRepository;
    @Mock
    private LhUnitCandidateExtractor lhUnitCandidateExtractor;
    @Mock
    private AnnouncementUnitSummaryService unitSummaryService;

    private NoticeImportPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new NoticeImportPersistenceService(
                announcementRepository,
                announcementDetailRepository,
                announcementCategoryRepository,
                announcementParseRawRepository,
                announcementEligibilityRepository,
                announcementUnitRepository,
                new AnnouncementNormalizer(),
                new CategoryDetector(),
                new SupplyCountParser(),
                lhUnitCandidateExtractor,
                unitSummaryService,
                new ObjectMapper()
        );
    }

    @Test
    void replaceUnitsDeletesExistingRowsBeforeSavingExtractedRowsOnEveryImport() {
        Announcement announcement = announcement();
        given(lhUnitCandidateExtractor.extract(eq("PAN-001"), any(), eq("국민임대"), eq("아파트"), eq("경기도")))
                .willReturn(List.of(candidate()));

        ReflectionTestUtils.invokeMethod(
                service,
                "replaceUnits",
                announcement,
                "PAN-001",
                LhDetailResponseFixtures.singleDsSbdOneUnit(),
                null
        );
        ReflectionTestUtils.invokeMethod(
                service,
                "replaceUnits",
                announcement,
                "PAN-001",
                LhDetailResponseFixtures.singleDsSbdOneUnit(),
                null
        );

        ArgumentCaptor<AnnouncementUnit> unitCaptor = ArgumentCaptor.forClass(AnnouncementUnit.class);
        verify(announcementUnitRepository, times(2)).deleteByAnnouncementId(1L);
        verify(announcementUnitRepository, times(2)).save(unitCaptor.capture());
        assertThat(unitCaptor.getAllValues())
                .extracting(AnnouncementUnit::getUnitSource)
                .containsOnly(AnnouncementUnitSource.LH_API);
        assertThat(unitCaptor.getValue().getSourceUnitKey()).isEqualTo("lh-unit-1");
        assertThat(unitCaptor.getValue().getSupplyHouseholdCount()).isEqualTo(30);
    }

    @Test
    void upsertLhRetiresDuplicateSourceRowsBeforeUpdatingCanonicalAnnouncement() throws Exception {
        Announcement canonical = announcement(1L, "기존 공고", LocalDateTime.now().minusDays(1));
        Announcement duplicate = announcement(2L, "중복 공고", LocalDateTime.now().minusDays(2));
        given(announcementRepository.findSourcePairCandidates(SourceType.LH, "PAN-001"))
                .willReturn(List.of(canonical, duplicate));
        given(announcementRepository.save(any(Announcement.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Announcement saved = service.upsertLh(lhItem("PAN-001", "새 공고명"));

        assertThat(saved).isSameAs(canonical);
        assertThat(saved.getNoticeName()).isEqualTo("새 공고명");
        assertThat(duplicate.isDeleted()).isTrue();
        assertThat(duplicate.getSourceNoticeId()).isEqualTo("PAN-001#DUPLICATE-2");
        then(announcementRepository).should().saveAll(List.of(duplicate));
    }

    @Test
    void upsertLhReusesSingleSourcePairCandidateForSamePanId() throws Exception {
        Announcement existing = announcement(1L, "기존 공고", LocalDateTime.now().minusDays(1));
        given(announcementRepository.findSourcePairCandidates(SourceType.LH, "PAN-001"))
                .willReturn(List.of(existing));
        given(announcementRepository.save(any(Announcement.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Announcement saved = service.upsertLh(lhItem("PAN-001", "새 공고명"));

        assertThat(saved).isSameAs(existing);
        assertThat(saved.getSourceNoticeId()).isEqualTo("PAN-001");
        assertThat(saved.getNoticeName()).isEqualTo("새 공고명");
    }

    @Test
    void preflightSourcePairDuplicatesReportsCanonicalAndDuplicateIds() {
        Announcement canonical = announcement(1L, "최신 공고", LocalDateTime.now());
        Announcement duplicate = announcement(2L, "중복 공고", LocalDateTime.now().minusDays(1));
        given(announcementRepository.findSourcePairCandidates(SourceType.LH, "PAN-001"))
                .willReturn(List.of(canonical, duplicate));

        NoticeImportPersistenceService.SourcePairDuplicatePreflight preflight =
                service.preflightSourcePairDuplicates(SourceType.LH, "PAN-001");

        assertThat(preflight.sourcePrimary()).isEqualTo(SourceType.LH);
        assertThat(preflight.sourceNoticeId()).isEqualTo("PAN-001");
        assertThat(preflight.canonical()).isSameAs(canonical);
        assertThat(preflight.hasDuplicates()).isTrue();
        assertThat(preflight.duplicateIds()).containsExactly(2L);
    }

    private Announcement announcement() {
        return announcement(1L, "테스트 공고", LocalDateTime.now());
    }

    private Announcement announcement(Long id, String noticeName, LocalDateTime collectedAt) {
        Announcement announcement = Announcement.builder()
                .sourcePrimary(SourceType.LH)
                .sourceNoticeId("PAN-001")
                .noticeName(noticeName)
                .providerName("LH")
                .sourceNoticeUrl("https://example.com/notice")
                .noticeStatus(AnnouncementStatus.OPEN)
                .regionLevel1("경기도")
                .supplyTypeRaw("국민임대")
                .supplyTypeNormalized("국민임대")
                .houseTypeRaw("아파트")
                .houseTypeNormalized("아파트")
                .matchKey("match-key")
                .merged(false)
                .collectedAt(collectedAt)
                .build();
        ReflectionTestUtils.setField(announcement, "id", id);
        return announcement;
    }

    private JsonNode lhItem(String panId, String noticeName) throws Exception {
        return new ObjectMapper().readTree("""
                {
                  "PAN_ID": "%s",
                  "PAN_NM": "%s",
                  "PAN_NT_ST_DT": "2025-01-01",
                  "CLSG_DT": "2025-01-31",
                  "DTL_URL": "https://example.com/notice",
                  "DTL_URL_MOB": "https://example.com/mobile",
                  "PAN_SS": "접수중",
                  "CNP_CD_NM": "경기도",
                  "AIS_TP_CD_NM": "국민임대"
                }
                """.formatted(panId, noticeName));
    }

    private LhUnitCandidate candidate() {
        return new LhUnitCandidate(
                "lh-unit-1",
                0,
                "테스트단지",
                "경기도 수원시 테스트로 1",
                "경기도",
                "수원시",
                "국민임대",
                "국민임대",
                "아파트",
                "아파트",
                "59.84㎡",
                new BigDecimal("59.84"),
                30,
                "LH 원문 행"
        );
    }
}
