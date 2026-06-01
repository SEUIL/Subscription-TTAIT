package com.ttait.subscription.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ttait.subscription.external.rtms.RtmsSourceType;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketPrepareRequest(
        RtmsSourceType sourceType,
        String dealYm,
        Integer numOfRows,
        Integer maxPages,
        String dealYmFrom,
        String dealYmTo,
        Integer minimumSampleCount,
        Boolean retryNoLawdCode
) {
    public RtmsSourceType sourceTypeOrDefault() {
        return sourceType == null ? RtmsSourceType.APT_RENT : sourceType;
    }

    public int numOfRowsOrDefault() {
        return numOfRows == null ? 100 : numOfRows;
    }

    public String dealYmFromOrDefault() {
        return dealYmFrom == null || dealYmFrom.isBlank() ? dealYm : dealYmFrom;
    }

    public String dealYmToOrDefault() {
        return dealYmTo == null || dealYmTo.isBlank() ? dealYm : dealYmTo;
    }

    public boolean retryNoLawdCodeOrDefault() {
        return Boolean.TRUE.equals(retryNoLawdCode);
    }
}
