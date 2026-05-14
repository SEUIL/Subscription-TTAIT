package com.ttait.subscription.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LhCandidateCollectionRequest(
        Integer page,
        Integer size
) {
}
