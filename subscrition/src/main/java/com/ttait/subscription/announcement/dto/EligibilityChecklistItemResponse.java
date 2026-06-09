package com.ttait.subscription.announcement.dto;

public record EligibilityChecklistItemResponse(
        String key,
        String group,
        String label,
        EligibilityCheckStatus status,
        String severity,
        String reason,
        String userValue,
        String announcementCondition,
        String actionLabel,
        String actionTarget
) {
}
