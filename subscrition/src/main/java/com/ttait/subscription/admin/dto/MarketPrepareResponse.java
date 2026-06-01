package com.ttait.subscription.admin.dto;

import java.math.BigDecimal;
import java.util.List;

public record MarketPrepareResponse(
        Long announcementId,
        String status,
        AddressNormalizationResponse normalization,
        int preparedBatchCount,
        int skippedUnitCount,
        List<UnitPreparation> units,
        List<BatchPreparation> batches
) {
    public record UnitPreparation(
            Long unitId,
            String sourceType,
            String lawdCd,
            BigDecimal areaMin,
            BigDecimal areaMax,
            String status,
            String blocker
    ) {
    }

    public record BatchPreparation(
            String sourceType,
            String lawdCd,
            String dealYm,
            String dealYmFrom,
            String dealYmTo,
            BigDecimal areaMin,
            BigDecimal areaMax,
            MarketRtmsSnapshotBatchResponse result
    ) {
    }
}
