package com.ttait.subscription.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LawdCodeMappingUpsertRequest(
        List<Item> mappings
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String regionLevel1,
            String regionLevel2,
            String legalDongName,
            String legalDongCode,
            Boolean active
    ) {
    }
}
