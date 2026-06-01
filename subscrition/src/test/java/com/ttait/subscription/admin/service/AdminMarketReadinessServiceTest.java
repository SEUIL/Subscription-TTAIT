package com.ttait.subscription.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ttait.subscription.admin.dto.MarketReadinessResponse;
import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.announcement.domain.AnnouncementUnitSource;
import com.ttait.subscription.announcement.domain.SourceType;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.announcement.repository.AnnouncementUnitRepository;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.external.rtms.RtmsProperties;
import com.ttait.subscription.market.domain.MarketPriceSnapshot;
import com.ttait.subscription.market.domain.MarketSnapshotStatus;
import com.ttait.subscription.market.domain.MarketSourceType;
import com.ttait.subscription.market.repository.MarketPriceSnapshotRepository;
import com.ttait.subscription.market.repository.MarketTransactionRawRepository;
import java.math.BigDecimal;
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
class AdminMarketReadinessServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;
    @Mock
    private AnnouncementUnitRepository unitRepository;
    @Mock
    private MarketPriceSnapshotRepository snapshotRepository;
    @Mock
    private MarketTransactionRawRepository rawRepository;

    private AdminMarketReadinessService service;

    @BeforeEach
    void setUp() {
        service = new AdminMarketReadinessService(
                announcementRepository,
                unitRepository,
                snapshotRepository,
                rawRepository,
                new RtmsProperties("test-service-key"));
    }

    @Test
    void readinessReportsMissingLawdCdBeforeSnapshotLookup() {
        AnnouncementUnit unit = unit(false, new BigDecimal("59.84"));
        given(announcementRepository.existsById(1L)).willReturn(true);
        given(unitRepository.findByAnnouncementIdAndDeletedFalseOrderByUnitOrderAsc(1L)).willReturn(List.of(unit));

        MarketReadinessResponse response = service.getReadiness(1L, MarketSourceType.APT_RENT, "202401", "202406");

        assertThat(response.rtmsServiceKeyConfigured()).isTrue();
        assertThat(response.readyUnitCount()).isZero();
        assertThat(response.blockedUnitCount()).isEqualTo(1);
        assertThat(response.units().get(0).blocker()).isEqualTo("UNIT_LAWD_CD_MISSING");
        assertThat(response.units().get(0).marketReady()).isFalse();
        then(rawRepository).should(never()).countBySourceTypeAndLawdCdAndDealYmBetweenAndExclusiveAreaBetween(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void readinessReportsReadyWhenSnapshotIsOk() {
        AnnouncementUnit unit = unit(true, new BigDecimal("59.84"));
        MarketPriceSnapshot snapshot = snapshot(MarketSnapshotStatus.OK);
        given(announcementRepository.existsById(1L)).willReturn(true);
        given(unitRepository.findByAnnouncementIdAndDeletedFalseOrderByUnitOrderAsc(1L)).willReturn(List.of(unit));
        given(rawRepository.countBySourceTypeAndLawdCdAndDealYmBetweenAndExclusiveAreaBetween(
                MarketSourceType.APT_RENT, "41570", "202401", "202406", new BigDecimal("59.84"), new BigDecimal("59.84")))
                .willReturn(12L);
        given(snapshotRepository.findFirstBySourceTypeAndLawdCdAndDealYmFromAndDealYmToAndAreaMinLessThanEqualAndAreaMaxGreaterThanEqualOrderByAggregatedAtDesc(
                MarketSourceType.APT_RENT, "41570", "202401", "202406", new BigDecimal("59.84"), new BigDecimal("59.84")))
                .willReturn(Optional.of(snapshot));

        MarketReadinessResponse response = service.getReadiness(1L, MarketSourceType.APT_RENT, "202401", "202406");

        MarketReadinessResponse.UnitReadiness unitReadiness = response.units().get(0);
        assertThat(response.readyUnitCount()).isEqualTo(1);
        assertThat(response.blockedUnitCount()).isZero();
        assertThat(unitReadiness.blocker()).isEqualTo("READY");
        assertThat(unitReadiness.rawTransactionCount()).isEqualTo(12L);
        assertThat(unitReadiness.snapshotFound()).isTrue();
        assertThat(unitReadiness.snapshotStatus()).isEqualTo(MarketSnapshotStatus.OK);
        assertThat(unitReadiness.marketReady()).isTrue();
    }

    @Test
    void readinessReportsInsufficientDataWhenSnapshotIsBelowThreshold() {
        AnnouncementUnit unit = unit(true, new BigDecimal("59.84"));
        given(announcementRepository.existsById(1L)).willReturn(true);
        given(unitRepository.findByAnnouncementIdAndDeletedFalseOrderByUnitOrderAsc(1L)).willReturn(List.of(unit));
        given(rawRepository.countBySourceTypeAndLawdCdAndDealYmBetweenAndExclusiveAreaBetween(
                MarketSourceType.APT_RENT, "41570", "202401", "202406", new BigDecimal("59.84"), new BigDecimal("59.84")))
                .willReturn(1L);
        given(snapshotRepository.findFirstBySourceTypeAndLawdCdAndDealYmFromAndDealYmToAndAreaMinLessThanEqualAndAreaMaxGreaterThanEqualOrderByAggregatedAtDesc(
                MarketSourceType.APT_RENT, "41570", "202401", "202406", new BigDecimal("59.84"), new BigDecimal("59.84")))
                .willReturn(Optional.of(snapshot(MarketSnapshotStatus.INSUFFICIENT_DATA)));

        MarketReadinessResponse response = service.getReadiness(1L, MarketSourceType.APT_RENT, "202401", "202406");

        assertThat(response.units().get(0).blocker()).isEqualTo("INSUFFICIENT_DATA");
        assertThat(response.units().get(0).marketReady()).isFalse();
    }

    @Test
    void readinessRejectsMissingAnnouncement() {
        given(announcementRepository.existsById(404L)).willReturn(false);

        assertThatThrownBy(() -> service.getReadiness(404L, MarketSourceType.APT_RENT, "202401", "202406"))
                .isInstanceOf(ApiException.class)
                .hasMessage("announcement not found");
    }

    @Test
    void readinessRejectsInvalidDealMonthAndWideRange() {
        assertThatThrownBy(() -> service.getReadiness(1L, MarketSourceType.APT_RENT, "202499", "202406"))
                .isInstanceOf(ApiException.class)
                .hasMessage("dealYmFrom must be a valid YYYYMM");
        assertThatThrownBy(() -> service.getReadiness(1L, MarketSourceType.APT_RENT, "202401", "202501"))
                .isInstanceOf(ApiException.class)
                .hasMessage("dealYm range must be within 12 months");
    }

    private AnnouncementUnit unit(boolean withLawdCd, BigDecimal exclusiveArea) {
        Announcement announcement = Announcement.builder()
                .sourcePrimary(SourceType.LH)
                .sourceNoticeId("LH-1")
                .noticeName("테스트 공고")
                .providerName("LH")
                .sourceNoticeUrl("https://example.com")
                .noticeStatus(AnnouncementStatus.OPEN)
                .regionLevel1("경기도")
                .regionLevel2("김포시")
                .matchKey("LH-1")
                .collectedAt(LocalDateTime.of(2026, 5, 26, 10, 0))
                .build();
        ReflectionTestUtils.setField(announcement, "id", 1L);
        AnnouncementUnit unit = AnnouncementUnit.builder()
                .announcement(announcement)
                .unitSource(AnnouncementUnitSource.LH_API)
                .sourceUnitKey("unit-1")
                .unitOrder(1)
                .complexName("테스트아파트")
                .fullAddress("경기도 김포시 마산동 1")
                .regionLevel1("경기도")
                .regionLevel2("김포시")
                .houseTypeNormalized("아파트")
                .exclusiveAreaValue(exclusiveArea)
                .depositAmount(75000L)
                .monthlyRentAmount(35L)
                .build();
        ReflectionTestUtils.setField(unit, "id", 20L);
        if (withLawdCd) {
            unit.markAddressResolved("경기도 김포시 마산동 1", "4157010100", "41570", LocalDateTime.of(2026, 5, 26, 11, 0));
        }
        return unit;
    }

    private MarketPriceSnapshot snapshot(MarketSnapshotStatus status) {
        MarketPriceSnapshot snapshot = MarketPriceSnapshot.builder()
                .sourceType(MarketSourceType.APT_RENT)
                .lawdCd("41570")
                .dealYmFrom("202401")
                .dealYmTo("202406")
                .areaMin(new BigDecimal("50.00"))
                .areaMax(new BigDecimal("70.00"))
                .sampleCount(5)
                .status(status)
                .snapshotKey("snapshot-key")
                .aggregatedAt(LocalDateTime.of(2026, 5, 26, 12, 0))
                .build();
        ReflectionTestUtils.setField(snapshot, "id", 30L);
        return snapshot;
    }
}
