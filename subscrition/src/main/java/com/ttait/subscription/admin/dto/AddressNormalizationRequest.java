package com.ttait.subscription.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AddressNormalizationRequest(
        Boolean retryNoLawdCode
) {

    public boolean retryNoLawdCodeOrDefault() {
        return Boolean.TRUE.equals(retryNoLawdCode);
    }
}
