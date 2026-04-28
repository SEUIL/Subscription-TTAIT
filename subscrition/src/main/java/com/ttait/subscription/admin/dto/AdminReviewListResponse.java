package com.ttait.subscription.admin.dto;

import com.ttait.subscription.announcement.domain.AnnouncementEligibility;
import com.ttait.subscription.announcement.domain.ParseReviewStatus;
import java.time.LocalDateTime;

public record AdminReviewListResponse(
        Long announcementId,             // 공고 PK
        String noticeName,               // 공고명
        String regionLevel1,             // 광역시/도
        ParseReviewStatus reviewStatus,  // 검수 상태
        LocalDateTime reviewedAt,        // 검수 처리 시각
        String reviewedBy                // 검수한 관리자 ID
) {
    public static AdminReviewListResponse from(AnnouncementEligibility eligibility) {
        return new AdminReviewListResponse(
                eligibility.getAnnouncement().getId(),
                eligibility.getAnnouncement().getNoticeName(),
                eligibility.getAnnouncement().getRegionLevel1(),
                eligibility.getReviewStatus(),
                eligibility.getReviewedAt(),
                eligibility.getReviewedBy()
        );
    }
}
