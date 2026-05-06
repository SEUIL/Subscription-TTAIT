package com.ttait.subscription.user.dto;

import com.ttait.subscription.user.domain.enums.CategoryCode;
import com.ttait.subscription.user.domain.enums.MaritalStatus;
import java.util.List;

public record UserProfileResponse(
        Long userId,
        String loginId,
        String email,
        String phone,
        String role,
        boolean profileCompleted,
        Integer age,
        MaritalStatus maritalStatus,
        Integer marriageYears,
        Integer childrenCount,
        Integer householdSize,
        boolean isHomeless,
        boolean isLowIncome,
        boolean isElderly,
        boolean isRecipient,
        boolean isNearPoverty,
        boolean isSingleParentFamily,
        Long monthlyAverageIncome,
        Long totalAssets,
        Long vehicleAssetAmount,
        String preferredRegionLevel1,
        String preferredRegionLevel2,
        String preferredHouseType,
        String preferredSupplyType,
        Long maxDeposit,
        Long maxMonthlyRent,
        List<CategoryCode> categories
) {
}
