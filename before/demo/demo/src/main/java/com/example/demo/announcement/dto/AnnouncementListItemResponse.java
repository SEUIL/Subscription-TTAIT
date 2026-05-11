package com.example.demo.announcement.dto;

import java.time.LocalDate;
import java.util.List;

public record AnnouncementListItemResponse(
        Long announcementId,
        String noticeName,
        String providerName,
        String supplyType,
        String houseType,
        String regionLevel1,
        String regionLevel2,
        String complexName,
        Long depositAmount,
        Long monthlyRentAmount,
        LocalDate applicationStartDate,
        LocalDate applicationEndDate,
        String noticeStatus,
        List<String> recommendedTags,
        boolean isSaved
) {
}
