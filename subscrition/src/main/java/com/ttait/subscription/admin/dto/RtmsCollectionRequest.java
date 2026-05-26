package com.ttait.subscription.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ttait.subscription.external.rtms.RtmsSourceType;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RtmsCollectionRequest(
        RtmsSourceType sourceType,
        String lawdCd,
        String dealYm,
        Integer pageNo,
        Integer numOfRows
) {

    public int pageNoOrDefault() {
        return pageNo == null ? 1 : pageNo;
    }

    public int numOfRowsOrDefault() {
        return numOfRows == null ? 100 : numOfRows;
    }
}
