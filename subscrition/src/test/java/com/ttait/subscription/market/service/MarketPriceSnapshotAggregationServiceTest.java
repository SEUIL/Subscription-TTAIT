package com.ttait.subscription.market.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.external.support.CanonicalJsonHasher;
import com.ttait.subscription.market.domain.MarketPriceSnapshot;
import com.ttait.subscription.market.domain.MarketSnapshotStatus;
import com.ttait.subscription.market.domain.MarketSourceType;
import com.ttait.subscription.market.domain.MarketTransactionRaw;
import com.ttait.subscription.market.repository.MarketPriceSnapshotRepository;
import com.ttait.subscription.market.repository.MarketTransactionRawRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MarketPriceSnapshotAggregationServiceTest {

    @Mock
    private MarketTransactionRawRepository rawRepository;

    @Mock
    private MarketPriceSnapshotRepository snapshotRepository;

    private MarketPriceSnapshotAggregationService service;

    @BeforeEach
    void setUp() {
        service = new MarketPriceSnapshotAggregationService(
                rawRepository,
                snapshotRepository,
                new CanonicalJsonHasher(new ObjectMapper())
        );
    }

    @Test
    void aggregatesRawTransactionsIntoSnapshot() {
        given(rawRepository.findBySourceTypeAndLawdCdAndDealYmBetweenAndExclusiveAreaBetween(
                MarketSourceType.APT_RENT,
                "41570",
                "202401",
                "202406",
                new BigDecimal("50.00"),
                new BigDecimal("70.00")
        )).willReturn(List.of(
                raw("hash-1", 60000L, 20L),
                raw("hash-2", 80000L, 40L),
                raw("hash-3", 70000L, 30L)
        ));
        given(snapshotRepository.findBySnapshotKey(anyString())).willReturn(Optional.empty());
        given(snapshotRepository.save(any(MarketPriceSnapshot.class))).willAnswer(invocation -> {
            MarketPriceSnapshot snapshot = invocation.getArgument(0);
            ReflectionTestUtils.setField(snapshot, "id", 7L);
            return snapshot;
        });

        MarketPriceSnapshotAggregationService.AggregationResult result = service.aggregate(
                MarketSourceType.APT_RENT,
                "41570",
                "202401",
                "202406",
                new BigDecimal("50.00"),
                new BigDecimal("70.00"),
                3
        );

        assertThat(result.snapshotId()).isEqualTo(7L);
        assertThat(result.sampleCount()).isEqualTo(3);
        assertThat(result.status()).isEqualTo(MarketSnapshotStatus.OK);
        assertThat(result.avgDepositAmount()).isEqualTo(70000L);
        assertThat(result.medianDepositAmount()).isEqualTo(70000L);
        assertThat(result.avgMonthlyRentAmount()).isEqualTo(30L);
        assertThat(result.medianMonthlyRentAmount()).isEqualTo(30L);
        assertThat(result.snapshotKey()).hasSize(64);

        ArgumentCaptor<MarketPriceSnapshot> snapshot = ArgumentCaptor.forClass(MarketPriceSnapshot.class);
        then(snapshotRepository).should().save(snapshot.capture());
        assertThat(snapshot.getValue().getAreaMin()).isEqualByComparingTo("50.00");
        assertThat(snapshot.getValue().getAreaMax()).isEqualByComparingTo("70.00");
    }

    @Test
    void marksInsufficientDataWhenSampleCountIsBelowThreshold() {
        given(rawRepository.findBySourceTypeAndLawdCdAndDealYmBetweenAndExclusiveAreaBetween(
                MarketSourceType.APT_RENT,
                "41570",
                "202401",
                "202406",
                new BigDecimal("50.00"),
                new BigDecimal("70.00")
        )).willReturn(List.of(raw("hash-1", 60000L, 20L)));
        given(snapshotRepository.findBySnapshotKey(anyString())).willReturn(Optional.empty());
        given(snapshotRepository.save(any(MarketPriceSnapshot.class))).willAnswer(invocation -> invocation.getArgument(0));

        MarketPriceSnapshotAggregationService.AggregationResult result = service.aggregate(
                MarketSourceType.APT_RENT,
                "41570",
                "202401",
                "202406",
                new BigDecimal("50.00"),
                new BigDecimal("70.00"),
                3
        );

        assertThat(result.sampleCount()).isEqualTo(1);
        assertThat(result.status()).isEqualTo(MarketSnapshotStatus.INSUFFICIENT_DATA);
    }

    @Test
    void rejectsInvalidDealYmRange() {
        assertThatThrownBy(() -> service.aggregate(
                MarketSourceType.APT_RENT,
                "41570",
                "202406",
                "202401",
                new BigDecimal("50.00"),
                new BigDecimal("70.00"),
                null
        )).isInstanceOf(ApiException.class)
                .hasMessage("dealYmFrom must be before or equal to dealYmTo");
    }

    private MarketTransactionRaw raw(String hash, Long depositAmount, Long monthlyRentAmount) {
        return MarketTransactionRaw.builder()
                .sourceType(MarketSourceType.APT_RENT)
                .lawdCd("41570")
                .dealYm("202405")
                .exclusiveArea(new BigDecimal("59.84"))
                .depositAmount(depositAmount)
                .monthlyRentAmount(monthlyRentAmount)
                .rawPayloadHash(hash)
                .rawPayload("raw")
                .collectedAt(LocalDateTime.of(2026, 5, 26, 10, 0))
                .build();
    }
}
