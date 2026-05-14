package com.ttait.subscription.external.service;

public record LhImportDedupeDecision(
        LhImportDecisionType decision,
        String reason,
        boolean shouldFetchDetail,
        boolean shouldParseGemini,
        boolean shouldPersistOfficial,
        boolean preserveExistingParsedData,
        Long announcementId,
        String panId,
        String lhItemHash,
        String lhDetailHash,
        String pdfUrl
) {
}
