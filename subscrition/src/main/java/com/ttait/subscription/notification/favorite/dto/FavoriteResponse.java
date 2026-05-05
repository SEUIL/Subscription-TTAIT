package com.ttait.subscription.notification.favorite.dto;

import com.ttait.subscription.notification.favorite.domain.UserFavoriteAnnouncement;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FavoriteResponse(
    Long announcementId,
    String noticeName,
    String providerName,
    String regionLevel1,
    String regionLevel2,
    Long depositAmount,
    Long monthlyRentAmount,
    LocalDate applicationEndDate,
    String noticeStatus,
    LocalDateTime favoritedAt
) {
    public static FavoriteResponse from(UserFavoriteAnnouncement favorite) {
        var a = favorite.getAnnouncement();
        return new FavoriteResponse(
            a.getId(),
            a.getNoticeName(),
            a.getProviderName(),
            a.getRegionLevel1(),
            a.getRegionLevel2(),
            a.getDepositAmount(),
            a.getMonthlyRentAmount(),
            a.getApplicationEndDate(),
            a.getNoticeStatus().name(),
            favorite.getCreatedAt()
        );
    }
}
