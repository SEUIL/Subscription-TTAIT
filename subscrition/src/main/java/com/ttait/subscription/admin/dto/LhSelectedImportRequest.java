package com.ttait.subscription.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LhSelectedImportRequest(
        List<Long> candidateIds,
        Boolean force
) {
    public boolean forceOrDefault() {
        return Boolean.TRUE.equals(force);
    }
}
