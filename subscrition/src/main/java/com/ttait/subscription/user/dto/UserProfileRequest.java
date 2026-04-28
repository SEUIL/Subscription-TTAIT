package com.ttait.subscription.user.dto;

import com.ttait.subscription.user.domain.enums.CategoryCode;
import com.ttait.subscription.user.domain.enums.MaritalStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UserProfileRequest(
        @NotNull @Min(0) @Max(120) Integer age,  // 나이
        @NotNull MaritalStatus maritalStatus,     // 혼인 상태
        @NotNull @Min(0) Integer childrenCount,   // 자녀 수
        boolean isHomeless,                       // 무주택 여부
        boolean isLowIncome,                      // 저소득 여부
        boolean isElderly,                        // 고령자 여부
        String preferredRegionLevel1,             // 희망 거주 지역 (광역시/도)
        String preferredRegionLevel2,             // 희망 거주 지역 (시/군/구)
        String preferredHouseType,                // 희망 주택 유형 (아파트/빌라 등)
        String preferredSupplyType,               // 희망 공급 유형 (공공임대/분양 등)
        Long maxDeposit,                          // 최대 보증금 (단위: 만원)
        Long maxMonthlyRent,                      // 최대 월세 (단위: 만원)
        List<CategoryCode> categories             // 해당하는 수혜 대상 카테고리 목록
) {
}
