package com.ttait.subscription.announcement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AnnouncementDetailResponse(
        Long announcementId,               // 공고 PK
        String noticeName,                 // 공고명
        String providerName,               // 공급 기관명
        String noticeStatus,               // 공고 상태
        LocalDate announcementDate,        // 공고 게시일
        LocalDate applicationStartDate,    // 청약 신청 시작일
        LocalDate applicationEndDate,      // 청약 신청 마감일
        LocalDate winnerAnnouncementDate,  // 당첨자 발표일
        String supplyType,                 // 공급 유형
        String houseType,                  // 주택 유형
        String complexName,                // 단지명
        String fullAddress,                // 전체 주소
        Long depositAmount,                // 보증금 (만원)
        Long monthlyRentAmount,            // 월세 (만원)
        Integer householdCount,            // 단지 전체 세대 수
        Integer supplyHouseholdCount,      // 공급 세대 수
        String heatingType,                // 난방 방식
        String exclusiveAreaText,          // 전용면적 (원문)
        BigDecimal exclusiveAreaValue,     // 전용면적 (㎡)
        String moveInExpectedYm,           // 입주 예정 연월
        String applicationDatetimeText,    // 청약 신청 일시 (원문)
        String guideText,                  // 안내 사항
        String contactPhone,               // 문의 전화번호
        String sourceUrl                   // 원본 공고 URL
) {
}
