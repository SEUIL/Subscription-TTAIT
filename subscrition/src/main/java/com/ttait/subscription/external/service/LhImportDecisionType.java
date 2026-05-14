package com.ttait.subscription.external.service;

public enum LhImportDecisionType {
    NEW,
    UNCHANGED_SKIP_GEMINI,
    CHANGED_REPARSE,
    FAILED_RETRY,
    FORCE_REPARSE,
    LAND_SKIP,
    NO_PDF
}
