package com.ttait.subscription.admin.dto;

public record AddressNormalizationResponse(
        Long announcementId,
        int processedCount,
        int successCount,
        int noAddressCount,
        int noLawdCodeCount
) {
}
