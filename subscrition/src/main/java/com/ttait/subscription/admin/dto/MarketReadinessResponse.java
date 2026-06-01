package com.ttait.subscription.admin.dto;

import com.ttait.subscription.market.domain.MarketSnapshotStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MarketReadinessResponse(
        Long announcementId,
        String sourceType,
        String dealYmFrom,
        String dealYmTo,
        boolean rtmsServiceKeyConfigured,
        long readyUnitCount,
        long blockedUnitCount,
        List<UnitReadiness> units
) {
    public record UnitReadiness(
            Long unitId,
            Integer unitOrder,
            String complexName,
            String fullAddress,
            String legalDongCode,
            String lawdCd,
            String addressStatus,
            String addressMessage,
            LocalDateTime addressNormalizedAt,
            BigDecimal exclusiveAreaValue,
            String recommendedSourceType,
            long rawTransactionCount,
            boolean snapshotFound,
            MarketSnapshotStatus snapshotStatus,
            boolean marketReady,
            String blocker
    ) {
    }
}
