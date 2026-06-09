package com.ttait.subscription.announcement.dto;

import java.util.List;

public record EligibilityChecklistResponse(
        Long announcementId,
        EligibilitySummaryStatus summaryStatus,
        String summaryMessage,
        int metCount,
        int notMetCount,
        int needsVerificationCount,
        int notApplicableCount,
        List<EligibilityChecklistItemResponse> items,
        String disclaimer
) {
}
