package com.ttait.subscription.external.rtms;

import java.math.BigDecimal;

public record RtmsTransactionItem(
        RtmsSourceType sourceType,
        String lawdCd,
        String dealYm,
        String legalDongName,
        String buildingName,
        String jibun,
        String roadName,
        Integer buildYear,
        BigDecimal exclusiveArea,
        Integer floor,
        Long depositAmount,
        Long monthlyRentAmount,
        Long tradeAmount,
        String rawPayload
) {
}
