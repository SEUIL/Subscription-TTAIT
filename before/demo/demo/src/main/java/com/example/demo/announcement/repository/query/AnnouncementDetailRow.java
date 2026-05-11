package com.example.demo.announcement.repository.query;

import com.example.demo.announcement.domain.AnnouncementStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AnnouncementDetailRow(
        Long announcementId,
        String noticeName,
        String providerName,
        AnnouncementStatus noticeStatus,
        LocalDate announcementDate,
        LocalDate applicationStartDate,
        LocalDate applicationEndDate,
        LocalDate winnerAnnouncementDate,
        String supplyType,
        String houseType,
        String complexName,
        String fullAddress,
        Long depositAmount,
        Long monthlyRentAmount,
        Integer householdCount,
        Integer supplyHouseholdCount,
        String heatingType,
        String exclusiveAreaText,
        BigDecimal exclusiveAreaValue,
        String moveInExpectedYm,
        String applicationDatetimeText,
        String guideText,
        String contactPhone,
        String sourceUrl
) {
}
