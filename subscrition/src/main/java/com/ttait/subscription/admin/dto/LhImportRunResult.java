package com.ttait.subscription.admin.dto;

public record LhImportRunResult(
        int fetched,
        int scanned,
        int skippedLand,
        int unchanged,
        int geminiSkipped,
        int imported,
        int reparsed,
        int failed
) {
}
