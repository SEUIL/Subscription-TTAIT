package com.ttait.subscription.admin.dto;

import com.ttait.subscription.market.domain.MarketSnapshotStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MarketSnapshotAggregateResponse(
        Long snapshotId,
        String sourceType,
        String lawdCd,
        String dealYmFrom,
        String dealYmTo,
        BigDecimal areaMin,
        BigDecimal areaMax,
        int sampleCount,
        Long avgDepositAmount,
        Long medianDepositAmount,
        Long avgMonthlyRentAmount,
        Long medianMonthlyRentAmount,
        Long avgTradeAmount,
        Long medianTradeAmount,
        MarketSnapshotStatus status,
        String snapshotKey,
        LocalDateTime aggregatedAt
) {
}
