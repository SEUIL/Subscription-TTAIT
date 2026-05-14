package com.ttait.subscription.admin.dto;

import java.util.List;

public record LhImportCandidateListResponse(
        List<LhImportCandidateResponse> candidates,
        long totalCount
) {
}
