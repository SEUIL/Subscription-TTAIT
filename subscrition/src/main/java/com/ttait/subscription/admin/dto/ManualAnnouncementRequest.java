package com.ttait.subscription.admin.dto;

import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

// 관리자 수동 공고 등록 요청 DTO
// 필수 필드만 @NotBlank/@NotNull로 강제, 나머지는 선택 입력
public record ManualAnnouncementRequest(

        @NotBlank String noticeName,         // 공고명
        @NotBlank String providerName,        // 공급 기관명
        @NotBlank String regionLevel1,        // 광역시/도 (예: 서울특별시)
        @NotNull AnnouncementStatus noticeStatus, // 공고 상태 (SCHEDULED/OPEN/CLOSED)

        Long depositAmount,           // 보증금 (만원)
        Long monthlyRentAmount,       // 월세 (만원)
        Integer supplyHouseholdCount, // 공급 세대 수

        String regionLevel2,          // 시/군/구 (선택)
        String fullAddress,           // 전체 주소 (선택)
        String complexName,           // 단지명 (선택)
        LocalDate applicationStartDate, // 청약 신청 시작일 (선택)
        LocalDate applicationEndDate    // 청약 신청 마감일 (선택)
) {}
