package com.ttait.subscription.announcement.dto;

import java.time.LocalDate;
import java.util.List;

public record RecommendationItemResponse(
        Long announcementId,
        String noticeName,
        String providerName,
        String supplyType,
        String houseType,
        String regionLevel1,
        String regionLevel2,
        String fullAddress,
        String complexName,
        Long depositAmount,
        Long monthlyRentAmount,
        LocalDate applicationStartDate,
        LocalDate applicationEndDate,
        String noticeStatus,
        String sourceNoticeUrl,
        int matchScore,
        List<String> matchReasons
) {
}
