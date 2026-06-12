package com.ttait.subscription.notification.favorite.dto;

import java.time.LocalDate;

public record FavoriteCalendarEventResponse(
    Long announcementId,
    String eventType,
    LocalDate date,
    String title,
    String priority
) {
}
