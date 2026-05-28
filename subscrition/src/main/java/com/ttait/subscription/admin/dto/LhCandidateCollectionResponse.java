package com.ttait.subscription.admin.dto;

import java.util.List;

public record LhCandidateCollectionResponse(
        int fetched,
        int scanned,
        int skippedLand,
        int skippedCommercial,
        List<LhImportCandidateResponse> candidates
) {

    public LhCandidateCollectionResponse(int fetched,
                                         int scanned,
                                         int skippedLand,
                                         List<LhImportCandidateResponse> candidates) {
        this(fetched, scanned, skippedLand, 0, candidates);
    }
}
