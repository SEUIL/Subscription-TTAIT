package com.ttait.subscription.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ttait.subscription.admin.dto.MarketRtmsSnapshotBatchRequest;
import com.ttait.subscription.admin.dto.MarketRtmsSnapshotBatchResponse;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.external.rtms.RtmsApiResult;
import com.ttait.subscription.external.rtms.RtmsSourceType;
import com.ttait.subscription.market.domain.MarketSnapshotStatus;
import com.ttait.subscription.market.domain.MarketSourceType;
import com.ttait.subscription.market.service.MarketPriceSnapshotAggregationService;
import com.ttait.subscription.market.service.MarketRtmsCollectionService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminMarketBatchServiceTest {

    @Mock
    private MarketRtmsCollectionService collectionService;
    @Mock
    private MarketPriceSnapshotAggregationService aggregationService;

    private AdminMarketBatchService service;

    @BeforeEach
    void setUp() {
        service = new AdminMarketBatchService(collectionService, aggregationService);
    }

    @Test
    void collectRtmsAndAggregateSnapshotRunsBothSteps() {
        given(collectionService.collectAll(RtmsSourceType.APT_RENT, "28200", "202405", 100, 10))
                .willReturn(new MarketRtmsCollectionService.CollectionAllResult(
                        RtmsSourceType.APT_RENT,
                        "28200",
                        "202405",
                        RtmsApiResult.Status.SUCCESS,
                        100,
                        96,
                        4,
                        0,
                        1,
                        830,
                        null
                ));
        given(aggregationService.aggregate(
                MarketSourceType.APT_RENT,
                "28200",
                "202405",
                "202405",
                new BigDecimal("35.0"),
                new BigDecimal("85.0"),
                3))
                .willReturn(new MarketPriceSnapshotAggregationService.AggregationResult(
                        7L,
                        MarketSourceType.APT_RENT,
                        "28200",
                        "202405",
                        "202405",
                        new BigDecimal("35.0"),
                        new BigDecimal("85.0"),
                        5,
                        70000L,
                        70000L,
                        30L,
                        30L,
                        null,
                        null,
                        MarketSnapshotStatus.OK,
                        "snapshot-key",
                        LocalDateTime.of(2026, 5, 26, 12, 0)
                ));

        MarketRtmsSnapshotBatchResponse response = service.collectRtmsAndAggregateSnapshot(new MarketRtmsSnapshotBatchRequest(
                RtmsSourceType.APT_RENT,
                "28200",
                "202405",
                100,
                10,
                null,
                null,
                new BigDecimal("35.0"),
                new BigDecimal("85.0"),
                3
        ));

        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.collection().savedCount()).isEqualTo(96);
        assertThat(response.snapshotAggregated()).isTrue();
        assertThat(response.snapshot().sampleCount()).isEqualTo(5);
        then(aggregationService).should().aggregate(
                MarketSourceType.APT_RENT,
                "28200",
                "202405",
                "202405",
                new BigDecimal("35.0"),
                new BigDecimal("85.0"),
                3);
    }

    @Test
    void collectRtmsAndAggregateSnapshotDoesNotAggregateWhenCollectionFails() {
        given(collectionService.collectAll(RtmsSourceType.APT_RENT, "28200", "202405", 100, null))
                .willReturn(new MarketRtmsCollectionService.CollectionAllResult(
                        RtmsSourceType.APT_RENT,
                        "28200",
                        "202405",
                        RtmsApiResult.Status.FAILED,
                        0,
                        0,
                        0,
                        1,
                        0,
                        null,
                        "service error"
                ));

        MarketRtmsSnapshotBatchResponse response = service.collectRtmsAndAggregateSnapshot(new MarketRtmsSnapshotBatchRequest(
                RtmsSourceType.APT_RENT,
                "28200",
                "202405",
                null,
                null,
                null,
                null,
                new BigDecimal("35.0"),
                new BigDecimal("85.0"),
                null
        ));

        assertThat(response.status()).isEqualTo("COLLECTION_FAILED");
        assertThat(response.snapshotAggregated()).isFalse();
        assertThat(response.snapshot()).isNull();
        then(aggregationService).should(never()).aggregate(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void collectRtmsAndAggregateSnapshotRejectsMissingAreaRange() {
        MarketRtmsSnapshotBatchRequest request = new MarketRtmsSnapshotBatchRequest(
                RtmsSourceType.APT_RENT,
                "28200",
                "202405",
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("85.0"),
                null
        );

        assertThatThrownBy(() -> service.collectRtmsAndAggregateSnapshot(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("areaMin and areaMax are required");
    }
}
