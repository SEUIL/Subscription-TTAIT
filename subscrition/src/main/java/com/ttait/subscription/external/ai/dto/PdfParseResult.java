package com.ttait.subscription.external.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PdfParseResult(
        Field applicationPeriod,       // 청약 신청 기간
        Field supplyHouseholdCount,    // 공급 세대 수
        Field depositMonthlyRent,      // 보증금/월세
        Field incomeAssetCriteria,     // 소득/자산 기준
        Field contact,                 // 문의처
        Long depositAmountManwon,      // 보증금 (단위: 만원)
        Long monthlyRentAmountManwon,  // 월세 (단위: 만원)
        Eligibility eligibility        // 자격 조건
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Field(
            String value,        // 추출된 값 (텍스트)
            Double confidence,   // AI 추출 신뢰도 (0.0~1.0)
            Integer sourcePage   // 출처 PDF 페이지 번호
    ) {

        public boolean hasValue() {
            return value != null && !value.isBlank();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Eligibility(
            Integer ageMin,               // 신청 가능 최소 나이
            Integer ageMax,               // 신청 가능 최대 나이
            String ageRawText,            // 나이 조건 원문 (AI 추출)
            String maritalTargetType,     // 혼인 상태 조건 유형
            Integer marriageYearLimit,    // 혼인 기간 제한 (년, 신혼부부 조건)
            String maritalRawText,        // 혼인 조건 원문 (AI 추출)
            Integer childrenMinCount,     // 자녀 최소 수 (다자녀 조건)
            String childrenRawText,       // 자녀 조건 원문 (AI 추출)
            Boolean homelessRequired,     // 무주택 조건 여부
            String homelessRawText,       // 무주택 조건 원문 (AI 추출)
            Boolean lowIncomeRequired,    // 저소득 조건 여부
            String incomeAssetCriteriaRaw, // 소득/자산 기준 원문 (AI 추출)
            Boolean elderlyRequired,      // 고령자 조건 여부
            Integer elderlyAgeMin,        // 고령자 최소 나이 기준
            String elderlyRawText,        // 고령자 조건 원문 (AI 추출)
            String eligibilityRaw,        // 전체 자격 조건 원문 (AI 추출)
            String specialSupplyRaw       // 특별공급 조건 원문 (AI 추출)
    ) {}
}
