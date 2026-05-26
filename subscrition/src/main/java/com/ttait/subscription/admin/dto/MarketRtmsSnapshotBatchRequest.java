package com.ttait.subscription.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ttait.subscription.external.rtms.RtmsSourceType;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketRtmsSnapshotBatchRequest(
        RtmsSourceType sourceType,
        String lawdCd,
        String dealYm,
        Integer numOfRows,
        Integer maxPages,
        String dealYmFrom,
        String dealYmTo,
        BigDecimal areaMin,
        BigDecimal areaMax,
        Integer minimumSampleCount
) {

    public int numOfRowsOrDefault() {
        return numOfRows == null ? 100 : numOfRows;
    }

    public String dealYmFromOrDefault() {
        return dealYmFrom == null || dealYmFrom.isBlank() ? dealYm : dealYmFrom;
    }

    public String dealYmToOrDefault() {
        return dealYmTo == null || dealYmTo.isBlank() ? dealYm : dealYmTo;
    }
}
