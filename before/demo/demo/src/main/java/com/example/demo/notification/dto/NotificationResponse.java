package com.example.demo.notification.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        Long announcementId,
        String notificationType,
        String title,
        String message,
        boolean isRead,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}
