package com.ttait.subscription.announcement.dto;

import java.time.LocalDate;

public record AnnouncementListItemResponse(
        Long announcementId,          // 공고 PK
        String noticeName,            // 공고명
        String providerName,          // 공급 기관명
        String supplyType,            // 공급 유형
        String houseType,             // 주택 유형
        String regionLevel1,          // 광역시/도
        String regionLevel2,          // 시/군/구
        String complexName,           // 단지명
        Long depositAmount,           // 보증금 (만원)
        Long monthlyRentAmount,       // 월세 (만원)
        LocalDate applicationStartDate, // 청약 신청 시작일
        LocalDate applicationEndDate,   // 청약 신청 마감일
        String noticeStatus           // 공고 상태
) {
}
