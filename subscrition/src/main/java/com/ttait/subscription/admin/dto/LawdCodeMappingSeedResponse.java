package com.ttait.subscription.admin.dto;

public record LawdCodeMappingSeedResponse(
        int dataLineCount,
        int parsedCount,
        int skippedCount,
        int insertedCount,
        int updatedCount
) {
}
