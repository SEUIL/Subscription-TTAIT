package com.ttait.subscription.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ttait.subscription.market.domain.MarketSourceType;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketSnapshotAggregateRequest(
        MarketSourceType sourceType,
        String lawdCd,
        String dealYmFrom,
        String dealYmTo,
        BigDecimal areaMin,
        BigDecimal areaMax,
        Integer minimumSampleCount
) {
}
