package com.ttait.subscription.market.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.announcement.domain.AnnouncementUnitSource;
import com.ttait.subscription.announcement.domain.SourceType;
import com.ttait.subscription.announcement.repository.AnnouncementUnitRepository;
import com.ttait.subscription.market.domain.MarketPriceSnapshot;
import com.ttait.subscription.market.domain.MarketSnapshotStatus;
import com.ttait.subscription.market.domain.MarketSourceType;
import com.ttait.subscription.market.dto.MarketComparisonResponse;
import com.ttait.subscription.market.dto.MarketComparisonStatus;
import com.ttait.subscription.market.repository.MarketPriceSnapshotRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MarketComparisonServiceTest {

    @Mock
    private AnnouncementUnitRepository unitRepository;

    @Mock
    private MarketPriceSnapshotRepository snapshotRepository;

    private MarketComparisonService service;

    @BeforeEach
    void setUp() {
        service = new MarketComparisonService(unitRepository, snapshotRepository);
    }

    @Test
    void comparesUnitRentAgainstMatchingSnapshot() {
        AnnouncementUnit unit = unit(true, new BigDecimal("59.84"));
        MarketPriceSnapshot snapshot = snapshot(MarketSnapshotStatus.OK);
        given(unitRepository.findByIdAndAnnouncementIdAndDeletedFalse(20L, 10L)).willReturn(Optional.of(unit));
        given(snapshotRepository
                .findFirstBySourceTypeAndLawdCdAndDealYmFromAndDealYmToAndAreaMinLessThanEqualAndAreaMaxGreaterThanEqualOrderByAggregatedAtDesc(
                        MarketSourceType.APT_RENT,
                        "41570",
                        "202401",
                        "202406",
                        new BigDecimal("59.84"),
                        new BigDecimal("59.84")
                )).willReturn(Optional.of(snapshot));

        MarketComparisonResponse response = service.compare(10L, 20L, MarketSourceType.APT_RENT, "202401", "202406");

        assertThat(response.status()).isEqualTo(MarketComparisonStatus.COMPARABLE);
        assertThat(response.snapshot().sampleCount()).isEqualTo(5);
        assertThat(response.depositComparison().unitAmount()).isEqualTo(75000L);
        assertThat(response.depositComparison().marketAmount()).isEqualTo(70000L);
        assertThat(response.depositComparison().differenceAmount()).isEqualTo(5000L);
        assertThat(response.depositComparison().differenceRatePercent()).isEqualByComparingTo("7.14");
        assertThat(response.monthlyRentComparison().differenceAmount()).isEqualTo(5L);
    }

    @Test
    void returnsMissingLawdCdStatusWithoutSnapshotLookup() {
        AnnouncementUnit unit = unit(false, new BigDecimal("59.84"));
        given(unitRepository.findByIdAndAnnouncementIdAndDeletedFalse(20L, 10L)).willReturn(Optional.of(unit));

        MarketComparisonResponse response = service.compare(10L, 20L, MarketSourceType.APT_RENT, "202401", "202406");

        assertThat(response.status()).isEqualTo(MarketComparisonStatus.UNIT_LAWD_CD_MISSING);
        assertThat(response.snapshot()).isNull();
        then(snapshotRepository).should(never())
                .findFirstBySourceTypeAndLawdCdAndDealYmFromAndDealYmToAndAreaMinLessThanEqualAndAreaMaxGreaterThanEqualOrderByAggregatedAtDesc(
                        any(), any(), any(), any(), any(), any());
    }

    @Test
    void returnsSnapshotNotFoundWhenNoSnapshotCoversArea() {
        AnnouncementUnit unit = unit(true, new BigDecimal("84.99"));
        given(unitRepository.findByIdAndAnnouncementIdAndDeletedFalse(20L, 10L)).willReturn(Optional.of(unit));
        given(snapshotRepository
                .findFirstBySourceTypeAndLawdCdAndDealYmFromAndDealYmToAndAreaMinLessThanEqualAndAreaMaxGreaterThanEqualOrderByAggregatedAtDesc(
                        MarketSourceType.APT_RENT,
                        "41570",
                        "202401",
                        "202406",
                        new BigDecimal("84.99"),
                        new BigDecimal("84.99")
                )).willReturn(Optional.empty());

        MarketComparisonResponse response = service.compare(10L, 20L, MarketSourceType.APT_RENT, "202401", "202406");

        assertThat(response.status()).isEqualTo(MarketComparisonStatus.SNAPSHOT_NOT_FOUND);
        assertThat(response.message()).isEqualTo("matching market snapshot not found");
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
        ReflectionTestUtils.setField(announcement, "id", 10L);

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
                .avgDepositAmount(70000L)
                .medianDepositAmount(71000L)
                .avgMonthlyRentAmount(30L)
                .medianMonthlyRentAmount(31L)
                .status(status)
                .snapshotKey("snapshot-key")
                .aggregatedAt(LocalDateTime.of(2026, 5, 26, 12, 0))
                .build();
        ReflectionTestUtils.setField(snapshot, "id", 30L);
        return snapshot;
    }
}
