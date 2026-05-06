package com.ttait.subscription.user.service;

import com.ttait.subscription.user.domain.User;
import com.ttait.subscription.user.domain.UserCategory;
import com.ttait.subscription.user.domain.UserProfile;
import com.ttait.subscription.user.dto.UserProfileResponse;
import java.util.List;

public final class UserProfileMapper {

    private UserProfileMapper() {
    }

    public static UserProfileResponse toResponse(User user, UserProfile profile, List<UserCategory> categories) {
        return new UserProfileResponse(
                user.getId(),
                user.getLoginId(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.isProfileCompleted(),
                profile != null ? profile.getAge() : null,
                profile != null ? profile.getMaritalStatus() : null,
                profile != null ? profile.getMarriageYears() : null,
                profile != null ? profile.getChildrenCount() : null,
                profile != null ? profile.getHouseholdSize() : null,
                profile != null && profile.isHomeless(),
                profile != null && profile.isLowIncome(),
                profile != null && profile.isElderly(),
                profile != null && profile.isRecipient(),
                profile != null && profile.isNearPoverty(),
                profile != null && profile.isSingleParentFamily(),
                profile != null ? profile.getMonthlyAverageIncome() : null,
                profile != null ? profile.getTotalAssets() : null,
                profile != null ? profile.getVehicleAssetAmount() : null,
                profile != null ? profile.getPreferredRegionLevel1() : null,
                profile != null ? profile.getPreferredRegionLevel2() : null,
                profile != null ? profile.getPreferredHouseType() : null,
                profile != null ? profile.getPreferredSupplyType() : null,
                profile != null ? profile.getMaxDeposit() : null,
                profile != null ? profile.getMaxMonthlyRent() : null,
                categories.stream().map(UserCategory::getCategoryCode).toList()
        );
    }
}
