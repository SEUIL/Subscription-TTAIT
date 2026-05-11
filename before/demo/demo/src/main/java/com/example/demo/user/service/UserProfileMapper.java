package com.example.demo.user.service;

import com.example.demo.user.domain.User;
import com.example.demo.user.domain.UserCategory;
import com.example.demo.user.domain.UserProfile;
import com.example.demo.user.dto.UserProfileResponse;
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
                profile != null ? profile.getAge() : null,
                profile != null ? profile.getMaritalStatus() : null,
                profile != null ? profile.getChildrenCount() : null,
                profile != null && profile.isHomeless(),
                profile != null && profile.isLowIncome(),
                profile != null && profile.isElderly(),
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
