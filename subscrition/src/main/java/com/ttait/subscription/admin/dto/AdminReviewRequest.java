package com.ttait.subscription.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AdminReviewRequest(
        String action,       // 검수 액션 (APPROVE/CORRECT/REJECT/REIMPORT)
        String note,         // 검수 메모
        Corrections corrections // 수정할 값 (CORRECT 액션 시 사용)
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Corrections(
            Long depositAmount,         // 수정할 보증금 (만원)
            Long monthlyRentAmount,     // 수정할 월세 (만원)
            Integer supplyHouseholdCount, // 수정할 공급 세대 수
            Integer ageMin,             // 수정할 최소 나이
            Integer ageMax,             // 수정할 최대 나이
            String maritalTargetType,   // 수정할 혼인 조건 유형
            Integer marriageYearLimit,  // 수정할 혼인 기간 제한 (년)
            Integer childrenMinCount,   // 수정할 자녀 최소 수
            Boolean homelessRequired,   // 수정할 무주택 조건 여부
            Boolean lowIncomeRequired,  // 수정할 저소득 조건 여부
            Boolean elderlyRequired,    // 수정할 고령자 조건 여부
            Integer elderlyAgeMin       // 수정할 고령자 최소 나이
    ) {}
}
