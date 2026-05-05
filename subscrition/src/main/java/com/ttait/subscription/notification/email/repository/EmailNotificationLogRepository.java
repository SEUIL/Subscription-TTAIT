package com.ttait.subscription.notification.email.repository;

import com.ttait.subscription.notification.email.domain.EmailNotificationLog;
import com.ttait.subscription.notification.email.domain.EmailNotificationType;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailNotificationLogRepository extends JpaRepository<EmailNotificationLog, Long> {

    @Query("SELECT l.announcementId FROM EmailNotificationLog l WHERE l.userId = :userId AND l.type = :type AND l.announcementId IN :announcementIds AND l.success = true")
    Set<Long> findAlreadySentAnnouncementIds(@Param("userId") Long userId,
                                              @Param("type") EmailNotificationType type,
                                              @Param("announcementIds") Collection<Long> announcementIds);

    boolean existsByUserIdAndAnnouncementIdAndType(Long userId, Long announcementId, EmailNotificationType type);
}
