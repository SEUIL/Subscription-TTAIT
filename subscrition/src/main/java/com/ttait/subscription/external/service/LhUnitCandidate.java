package com.ttait.subscription.external.service;

import java.math.BigDecimal;

public record LhUnitCandidate(
        String sourceUnitKey,
        int unitOrder,
        String complexName,
        String fullAddress,
        String regionLevel1,
        String regionLevel2,
        String supplyTypeRaw,
        String supplyTypeNormalized,
        String houseTypeRaw,
        String houseTypeNormalized,
        String exclusiveAreaText,
        BigDecimal exclusiveAreaValue,
        Integer supplyHouseholdCount,
        String rawText
) {
}
