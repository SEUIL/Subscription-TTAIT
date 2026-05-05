package com.ttait.subscription.notification.email.domain;

import com.ttait.subscription.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "email_notification_log",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_enl_user_announcement_type",
        columnNames = {"user_id", "announcement_id", "type"}
    ),
    indexes = @Index(name = "idx_enl_user_type", columnList = "user_id, type")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailNotificationLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "announcement_id", nullable = false)
    private Long announcementId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private EmailNotificationType type;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Builder
    public EmailNotificationLog(Long userId, Long announcementId, EmailNotificationType type,
                                LocalDateTime sentAt, boolean success, String errorMessage) {
        this.userId = userId;
        this.announcementId = announcementId;
        this.type = type;
        this.sentAt = sentAt;
        this.success = success;
        this.errorMessage = errorMessage;
    }
}
