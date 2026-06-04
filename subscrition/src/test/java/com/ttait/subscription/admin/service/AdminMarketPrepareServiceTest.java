package com.ttait.subscription.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ttait.subscription.admin.dto.MarketPrepareRequest;
import com.ttait.subscription.admin.dto.MarketPrepareResponse;
import com.ttait.subscription.admin.dto.MarketRtmsSnapshotBatchRequest;
import com.ttait.subscription.admin.dto.MarketRtmsSnapshotBatchResponse;
import com.ttait.subscription.admin.dto.MarketSnapshotAggregateResponse;
import com.ttait.subscription.admin.dto.RtmsCollectionAllResponse;
import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.announcement.domain.AnnouncementUnitSource;
import com.ttait.subscription.announcement.domain.SourceType;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.announcement.repository.AnnouncementUnitRepository;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.external.rtms.RtmsSourceType;
import com.ttait.subscription.market.domain.MarketSnapshotStatus;
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
class AdminMarketPrepareServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;
    @Mock
    private AnnouncementUnitRepository unitRepository;
    @Mock
    private AdminMarketAddressService addressService;
    @Mock
    private AdminMarketBatchService batchService;

    private AdminMarketPrepareService service;

    @BeforeEach
    void setUp() {
        service = new AdminMarketPrepareService(announcementRepository, unitRepository, addressService, batchService);
    }

    @Test
    void prepareRunsBatchForEligibleUnitsAndSkipsBlockedUnits() {
        given(announcementRepository.existsById(1L)).willReturn(true);
        AnnouncementUnit ready = unit(20L, true, new BigDecimal("59.84"));
        AnnouncementUnit missingLawd = unit(21L, false, new BigDecimal("59.84"));
        given(unitRepository.findWithAnnouncementByAnnouncementIdAndDeletedFalseOrderByUnitOrderAsc(1L))
                .willReturn(List.of(ready, missingLawd));
        given(batchService.collectRtmsAndAggregateSnapshot(any()))
                .willReturn(successBatchResponse());

        MarketPrepareResponse response = service.prepare(1L, new MarketPrepareRequest(
                RtmsSourceType.APT_RENT,
                "202406",
                100,
                10,
                "202401",
                "202406",
                3,
                false
        ));

        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.preparedBatchCount()).isEqualTo(1);
        assertThat(response.skippedUnitCount()).isEqualTo(1);
        assertThat(response.units()).extracting(MarketPrepareResponse.UnitPreparation::status)
                .containsExactly("QUEUED", "SKIPPED");
        assertThat(response.units().get(1).blocker()).isEqualTo("UNIT_LAWD_CD_MISSING");
        ArgumentCaptor<MarketRtmsSnapshotBatchRequest> requestCaptor = ArgumentCaptor.forClass(MarketRtmsSnapshotBatchRequest.class);
        then(batchService).should().collectRtmsAndAggregateSnapshot(requestCaptor.capture());
        MarketRtmsSnapshotBatchRequest batchRequest = requestCaptor.getValue();
        assertThat(batchRequest.sourceType()).isEqualTo(RtmsSourceType.APT_RENT);
        assertThat(batchRequest.lawdCd()).isEqualTo("41570");
        assertThat(batchRequest.areaMin()).isEqualByComparingTo("59.84");
        then(addressService).should(never()).normalizeAnnouncementUnits(any(), any(Boolean.class));
    }

    @Test
    void prepareCanNormalizeBeforeBatching() {
        given(announcementRepository.existsById(1L)).willReturn(true);
        AnnouncementUnit ready = unit(20L, true, new BigDecimal("59.84"));
        given(unitRepository.findWithAnnouncementByAnnouncementIdAndDeletedFalseOrderByUnitOrderAsc(1L)).willReturn(List.of(ready));
        given(batchService.collectRtmsAndAggregateSnapshot(any())).willReturn(successBatchResponse());

        service.prepare(1L, new MarketPrepareRequest(
                RtmsSourceType.APT_RENT,
                "202406",
                null,
                null,
                null,
                null,
                null,
                true
        ));

        then(addressService).should().normalizeAnnouncementUnits(1L, true);
    }

    @Test
    void prepareUsesUnitRecommendedSourceTypeForMixedHouseTypes() {
        given(announcementRepository.existsById(1L)).willReturn(true);
        AnnouncementUnit apartment = unit(20L, true, new BigDecimal("59.84"), "아파트");
        AnnouncementUnit officetel = unit(21L, true, new BigDecimal("29.90"), "오피스텔");
        given(unitRepository.findWithAnnouncementByAnnouncementIdAndDeletedFalseOrderByUnitOrderAsc(1L))
                .willReturn(List.of(apartment, officetel));
        given(batchService.collectRtmsAndAggregateSnapshot(any()))
                .willReturn(successBatchResponse());

        MarketPrepareResponse response = service.prepare(1L, new MarketPrepareRequest(
                RtmsSourceType.APT_RENT,
                "202406",
                100,
                10,
                "202401",
                "202406",
                3,
                false
        ));

        assertThat(response.preparedBatchCount()).isEqualTo(2);
        assertThat(response.units()).extracting(MarketPrepareResponse.UnitPreparation::sourceType)
                .containsExactly("APT_RENT", "OFFICETEL_RENT");
        ArgumentCaptor<MarketRtmsSnapshotBatchRequest> requestCaptor = ArgumentCaptor.forClass(MarketRtmsSnapshotBatchRequest.class);
        then(batchService).should(org.mockito.Mockito.times(2)).collectRtmsAndAggregateSnapshot(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues()).extracting(MarketRtmsSnapshotBatchRequest::sourceType)
                .containsExactly(RtmsSourceType.APT_RENT, RtmsSourceType.OFFICETEL_RENT);
    }

    @Test
    void prepareRejectsMissingAnnouncement() {
        given(announcementRepository.existsById(404L)).willReturn(false);

        assertThatThrownBy(() -> service.prepare(404L, new MarketPrepareRequest(
                RtmsSourceType.APT_RENT,
                "202406",
                100,
                10,
                "202401",
                "202406",
                3,
                false
        )))
                .isInstanceOf(ApiException.class)
                .hasMessage("announcement not found");
    }

    @Test
    void prepareRejectsUnboundedExternalCallInputs() {
        assertThatThrownBy(() -> service.prepare(1L, new MarketPrepareRequest(
                RtmsSourceType.APT_RENT, "202406", 101, 10, "202401", "202406", 3, false)))
                .isInstanceOf(ApiException.class)
                .hasMessage("numOfRows must be between 1 and 100");
        assertThatThrownBy(() -> service.prepare(1L, new MarketPrepareRequest(
                RtmsSourceType.APT_RENT, "202406", 100, 11, "202401", "202406", 3, false)))
                .isInstanceOf(ApiException.class)
                .hasMessage("maxPages must be between 1 and 10");
        assertThatThrownBy(() -> service.prepare(1L, new MarketPrepareRequest(
                RtmsSourceType.APT_RENT, "202499", 100, 10, "202401", "202406", 3, false)))
                .isInstanceOf(ApiException.class)
                .hasMessage("dealYm must be a valid YYYYMM");
    }

    @Test
    void prepareDistinguishesExistingAnnouncementWithNoUnits() {
        given(announcementRepository.existsById(1L)).willReturn(true);
        given(unitRepository.findWithAnnouncementByAnnouncementIdAndDeletedFalseOrderByUnitOrderAsc(1L)).willReturn(List.of());

        MarketPrepareResponse response = service.prepare(1L, new MarketPrepareRequest(
                RtmsSourceType.APT_RENT, "202406", 100, 10, "202401", "202406", 3, false));

        assertThat(response.status()).isEqualTo("NO_ELIGIBLE_UNITS");
        assertThat(response.preparedBatchCount()).isZero();
    }

    private MarketRtmsSnapshotBatchResponse successBatchResponse() {
        return new MarketRtmsSnapshotBatchResponse(
                "SUCCESS",
                new RtmsCollectionAllResponse("APT_RENT", "41570", "202406", "SUCCESS", 10, 10, 0, 0, 1, 10, null),
                new MarketSnapshotAggregateResponse(
                        7L,
                        "APT_RENT",
                        "41570",
                        "202401",
                        "202406",
                        new BigDecimal("59.84"),
                        new BigDecimal("59.84"),
                        10,
                        70000L,
                        70000L,
                        30L,
                        30L,
                        null,
                        null,
                        MarketSnapshotStatus.OK,
                        "snapshot-key",
                        LocalDateTime.of(2026, 5, 26, 12, 0)
                ),
                true,
                null
        );
    }

    private AnnouncementUnit unit(Long unitId, boolean withLawdCd, BigDecimal exclusiveArea) {
        return unit(unitId, withLawdCd, exclusiveArea, "아파트");
    }

    private AnnouncementUnit unit(Long unitId, boolean withLawdCd, BigDecimal exclusiveArea, String houseTypeNormalized) {
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
                .sourceUnitKey("unit-" + unitId)
                .unitOrder(unitId.intValue())
                .complexName("테스트아파트")
                .fullAddress("경기도 김포시 마산동 1")
                .regionLevel1("경기도")
                .regionLevel2("김포시")
                .houseTypeNormalized(houseTypeNormalized)
                .exclusiveAreaValue(exclusiveArea)
                .build();
        ReflectionTestUtils.setField(unit, "id", unitId);
        if (withLawdCd) {
            unit.markAddressResolved("경기도 김포시 마산동 1", "4157010100", "41570", LocalDateTime.of(2026, 5, 26, 11, 0));
        }
        return unit;
    }
}
