package com.ttait.subscription.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ttait.subscription.external.rtms.RtmsSourceType;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RtmsCollectionAllRequest(
        RtmsSourceType sourceType,
        String lawdCd,
        String dealYm,
        Integer numOfRows,
        Integer maxPages
) {

    public int numOfRowsOrDefault() {
        return numOfRows == null ? 100 : numOfRows;
    }
}
