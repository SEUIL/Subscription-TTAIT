package com.ttait.subscription.announcement.repository;

import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.domain.ParseReviewStatus;
import com.ttait.subscription.user.domain.enums.CategoryCode;
import java.util.Collection;
import java.util.List;

public record AnnouncementSearchCondition(
        String regionLevel1,
        String regionLevel2,
        String supplyType,
        String houseType,
        String provider,
        AnnouncementStatus status,
        String keyword,
        Long minDeposit,
        Long maxDeposit,
        Long minMonthlyRent,
        Long maxMonthlyRent,
        List<CategoryCode> categories,
        Collection<ParseReviewStatus> reviewStatuses
) {
}
