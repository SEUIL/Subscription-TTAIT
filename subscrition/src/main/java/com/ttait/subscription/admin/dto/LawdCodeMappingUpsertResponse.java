package com.ttait.subscription.admin.dto;

public record LawdCodeMappingUpsertResponse(
        int requestedCount,
        int insertedCount,
        int updatedCount
) {
}
