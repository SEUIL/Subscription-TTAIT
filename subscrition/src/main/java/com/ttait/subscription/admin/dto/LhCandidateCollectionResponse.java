package com.ttait.subscription.admin.dto;

import java.util.List;

public record LhCandidateCollectionResponse(
        int fetched,
        int scanned,
        int skippedLand,
        List<LhImportCandidateResponse> candidates
) {
}
