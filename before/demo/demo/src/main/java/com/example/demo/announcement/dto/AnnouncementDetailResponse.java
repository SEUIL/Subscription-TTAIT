package com.example.demo.announcement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AnnouncementDetailResponse(
        Long announcementId,
        String noticeName,
        String providerName,
        String noticeStatus,
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
        List<AttachmentResponse> attachments,
        String sourceUrl,
        MarketComparisonResponse marketComparison,
        List<String> recommendReasons
) {
}
