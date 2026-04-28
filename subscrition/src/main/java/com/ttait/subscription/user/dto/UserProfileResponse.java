package com.ttait.subscription.user.dto;

import com.ttait.subscription.user.domain.enums.CategoryCode;
import com.ttait.subscription.user.domain.enums.MaritalStatus;
import java.util.List;

public record UserProfileResponse(
        Long userId,                   // 사용자 PK
        String loginId,                // 로그인 ID
        String email,                  // 이메일
        String phone,                  // 휴대폰 번호
        Integer age,                   // 나이
        MaritalStatus maritalStatus,   // 혼인 상태
        Integer childrenCount,         // 자녀 수
        boolean isHomeless,            // 무주택 여부
        boolean isLowIncome,           // 저소득 여부
        boolean isElderly,             // 고령자 여부
        String preferredRegionLevel1,  // 희망 거주 지역 (광역시/도)
        String preferredRegionLevel2,  // 희망 거주 지역 (시/군/구)
        String preferredHouseType,     // 희망 주택 유형
        String preferredSupplyType,    // 희망 공급 유형
        Long maxDeposit,               // 최대 보증금 (단위: 만원)
        Long maxMonthlyRent,           // 최대 월세 (단위: 만원)
        List<CategoryCode> categories  // 수혜 대상 카테고리 목록
) {
}
