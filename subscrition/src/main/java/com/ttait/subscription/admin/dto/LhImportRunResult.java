package com.ttait.subscription.admin.dto;

public record LhImportRunResult(
        int fetched,
        int scanned,
        int skippedLand,
        int skippedCommercial,
        int unchanged,
        int geminiSkipped,
        int imported,
        int reparsed,
        int failed
) {

    public LhImportRunResult(int fetched,
                             int scanned,
                             int skippedLand,
                             int unchanged,
                             int geminiSkipped,
                             int imported,
                             int reparsed,
                             int failed) {
        this(fetched, scanned, skippedLand, 0, unchanged, geminiSkipped, imported, reparsed, failed);
    }
}
