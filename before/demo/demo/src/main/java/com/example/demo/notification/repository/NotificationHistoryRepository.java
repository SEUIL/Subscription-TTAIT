package com.example.demo.notification.repository;

import com.example.demo.notification.domain.NotificationHistory;
import com.example.demo.notification.domain.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {
    boolean existsByUserIdAndAnnouncementIdAndNotificationType(Long userId, Long announcementId, NotificationType notificationType);
}
