package com.ttait.subscription.notification.favorite.dto;

public record FavoriteScheduleSummaryResponse(
    int totalCount,
    int returnedCount,
    boolean truncated,
    int dueTodayCount,
    int dueTomorrowCount,
    int dueSoonCount,
    int openCount,
    int upcomingCount,
    int dateUnknownCount,
    int closedCount
) {
}
