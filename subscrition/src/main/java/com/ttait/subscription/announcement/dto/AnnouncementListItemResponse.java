package com.ttait.subscription.announcement.dto;

import java.time.LocalDate;

public record AnnouncementListItemResponse(
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
        boolean favorited
) {
}
