package com.ttait.subscription.admin.dto;

public record RtmsCollectionAllResponse(
        String sourceType,
        String lawdCd,
        String dealYm,
        String status,
        int fetchedCount,
        int savedCount,
        int duplicateCount,
        int failedCount,
        int collectedPageCount,
        Integer totalCount,
        String message
) {
}
