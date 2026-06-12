package com.ttait.subscription.notification.favorite.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FavoriteScheduleItemResponse(
    Long announcementId,
    String noticeName,
    String providerName,
    String noticeStatus,
    LocalDate applicationStartDate,
    LocalDate applicationEndDate,
    LocalDate announcementDate,
    LocalDate winnerAnnouncementDate,
    LocalDateTime favoritedAt,
    Integer dDay,
    String dDayLabel,
    FavoriteScheduleStatus scheduleStatus,
    String statusMessage,
    String actionLabel
) {
}
