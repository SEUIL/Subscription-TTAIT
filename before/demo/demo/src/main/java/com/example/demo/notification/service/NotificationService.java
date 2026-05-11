package com.example.demo.notification.service;

import com.example.demo.common.exception.ApiException;
import com.example.demo.notification.domain.Notification;
import com.example.demo.notification.dto.NotificationResponse;
import com.example.demo.notification.repository.NotificationRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(notification -> new NotificationResponse(
                        notification.getId(),
                        notification.getAnnouncement().getId(),
                        notification.getNotificationType().name(),
                        notification.getTitle(),
                        notification.getMessage(),
                        notification.isRead(),
                        notification.getCreatedAt(),
                        notification.getReadAt()))
                .toList();
    }

    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "notification not found"));
        notification.markAsRead();
    }
}
