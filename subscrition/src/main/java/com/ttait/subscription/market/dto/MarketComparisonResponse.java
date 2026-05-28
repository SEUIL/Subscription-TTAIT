package com.ttait.subscription.market.dto;

import com.ttait.subscription.market.domain.MarketSnapshotStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MarketComparisonResponse(
        Long announcementId,
        Long unitId,
        String sourceType,
        String lawdCd,
        String dealYmFrom,
        String dealYmTo,
        BigDecimal exclusiveArea,
        MarketComparisonStatus status,
        String message,
        UnitPrice unitPrice,
        SnapshotPrice snapshot,
        PriceDifference depositComparison,
        PriceDifference monthlyRentComparison,
        PriceDifference tradeComparison
) {

    public record UnitPrice(
            Long depositAmount,
            Long monthlyRentAmount,
            Long salePriceMin,
            Long salePriceMax
    ) {
    }

    public record SnapshotPrice(
            Long snapshotId,
            int sampleCount,
            MarketSnapshotStatus snapshotStatus,
            BigDecimal areaMin,
            BigDecimal areaMax,
            Long avgDepositAmount,
            Long medianDepositAmount,
            Long avgMonthlyRentAmount,
            Long medianMonthlyRentAmount,
            Long avgTradeAmount,
            Long medianTradeAmount,
            LocalDateTime aggregatedAt
    ) {
    }

    public record PriceDifference(
            Long unitAmount,
            Long marketAmount,
            Long differenceAmount,
            BigDecimal differenceRatePercent
    ) {
    }
}
