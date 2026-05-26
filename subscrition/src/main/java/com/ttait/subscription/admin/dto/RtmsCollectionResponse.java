package com.ttait.subscription.admin.dto;

public record RtmsCollectionResponse(
        String sourceType,
        String lawdCd,
        String dealYm,
        String status,
        int fetchedCount,
        int savedCount,
        int duplicateCount,
        int failedCount,
        String message
) {
}
