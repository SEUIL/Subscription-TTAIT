package com.ttait.subscription.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LawdCodeMappingSeedRequest(
        String content,
        String delimiter,
        Boolean activeOnly
) {

    public boolean activeOnlyOrDefault() {
        return activeOnly == null || activeOnly;
    }
}
