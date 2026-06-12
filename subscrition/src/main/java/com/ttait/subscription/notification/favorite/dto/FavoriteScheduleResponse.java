package com.ttait.subscription.notification.favorite.dto;

import java.util.List;

public record FavoriteScheduleResponse(
    FavoriteScheduleSummaryResponse summary,
    List<FavoriteScheduleGroupResponse> groups,
    List<FavoriteCalendarEventResponse> calendarEvents,
    String disclaimer
) {
}
