package com.ttait.subscription.admin.dto;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementDetail;
import com.ttait.subscription.announcement.domain.AnnouncementEligibility;
import com.ttait.subscription.announcement.domain.ConfidenceLevel;
import com.ttait.subscription.announcement.domain.MaritalTargetType;
import com.ttait.subscription.announcement.domain.ParseReviewStatus;
import java.time.LocalDateTime;

public record AdminReviewDetailResponse(
        Long announcementId,   // 공고 PK
        String noticeName,     // 공고명
        String providerName,   // 공급 기관명
        String regionLevel1,   // 광역시/도

        Long depositAmount,           // 보증금 (만원, AI 파싱 결과)
        Long monthlyRentAmount,       // 월세 (만원, AI 파싱 결과)
        Integer supplyHouseholdCount, // 공급 세대 수 (AI 파싱 결과)

        String depositMonthlyRentRaw,               // 보증금/월세 AI 추출 원문
        String supplyHouseholdCountRaw,             // 공급 세대 수 AI 추출 원문
        String supplyHouseholdCountBasis,           // 공급 세대 수 추출 근거 (AI 설명)
        ConfidenceLevel supplyHouseholdCountConfidence, // 공급 세대 수 추출 신뢰도

        Integer ageMin,                      // 최소 나이 조건
        Integer ageMax,                      // 최대 나이 조건
        String ageRawText,                   // 나이 조건 원문
        MaritalTargetType maritalTargetType, // 혼인 조건 유형
        Integer marriageYearLimit,           // 혼인 기간 제한 (년)
        String maritalRawText,               // 혼인 조건 원문
        Integer childrenMinCount,            // 자녀 최소 수
        String childrenRawText,              // 자녀 조건 원문
        Boolean homelessRequired,            // 무주택 조건 여부
        String homelessRawText,              // 무주택 조건 원문
        Boolean lowIncomeRequired,           // 저소득 조건 여부
        String incomeAssetCriteriaRaw,       // 소득/자산 기준 원문
        Boolean elderlyRequired,             // 고령자 조건 여부
        Integer elderlyAgeMin,               // 고령자 최소 나이
        String elderlyRawText,               // 고령자 조건 원문
        String eligibilityRaw,               // 전체 자격 조건 원문
        String specialSupplyRaw,             // 특별공급 조건 원문

        ParseReviewStatus reviewStatus, // 검수 상태
        String reviewedBy,              // 검수한 관리자 ID
        LocalDateTime reviewedAt,       // 검수 처리 시각
        String reviewNote               // 검수 메모
) {
    public static AdminReviewDetailResponse from(Announcement announcement, AnnouncementDetail detail,
                                                  AnnouncementEligibility eligibility) {
        return new AdminReviewDetailResponse(
                announcement.getId(),
                announcement.getNoticeName(),
                announcement.getProviderName(),
                announcement.getRegionLevel1(),

                announcement.getDepositAmount(),
                announcement.getMonthlyRentAmount(),
                announcement.getSupplyHouseholdCount(),

                detail != null ? detail.getDepositMonthlyRentRaw() : null,
                detail != null ? detail.getSupplyHouseholdCountRaw() : null,
                detail != null ? detail.getSupplyHouseholdCountBasis() : null,
                detail != null ? detail.getSupplyHouseholdCountConfidence() : null,

                eligibility.getAgeMin(),
                eligibility.getAgeMax(),
                eligibility.getAgeRawText(),
                eligibility.getMaritalTargetType(),
                eligibility.getMarriageYearLimit(),
                eligibility.getMaritalRawText(),
                eligibility.getChildrenMinCount(),
                eligibility.getChildrenRawText(),
                eligibility.getHomelessRequired(),
                eligibility.getHomelessRawText(),
                eligibility.getLowIncomeRequired(),
                eligibility.getIncomeAssetCriteriaRaw(),
                eligibility.getElderlyRequired(),
                eligibility.getElderlyAgeMin(),
                eligibility.getElderlyRawText(),
                eligibility.getEligibilityRaw(),
                eligibility.getSpecialSupplyRaw(),

                eligibility.getReviewStatus(),
                eligibility.getReviewedBy(),
                eligibility.getReviewedAt(),
                eligibility.getReviewNote()
        );
    }
}
