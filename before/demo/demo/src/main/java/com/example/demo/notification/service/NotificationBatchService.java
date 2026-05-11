package com.example.demo.notification.service;

import com.example.demo.announcement.domain.Announcement;
import com.example.demo.announcement.repository.AnnouncementRepository;
import com.example.demo.notification.domain.Notification;
import com.example.demo.notification.domain.NotificationHistory;
import com.example.demo.notification.domain.NotificationType;
import com.example.demo.notification.domain.UserSavedAnnouncement;
import com.example.demo.notification.repository.NotificationHistoryRepository;
import com.example.demo.notification.repository.NotificationRepository;
import com.example.demo.notification.repository.UserSavedAnnouncementRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationBatchService {

    private final AnnouncementRepository announcementRepository;
    private final UserSavedAnnouncementRepository userSavedAnnouncementRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;

    public NotificationBatchService(AnnouncementRepository announcementRepository,
                                    UserSavedAnnouncementRepository userSavedAnnouncementRepository,
                                    NotificationRepository notificationRepository,
                                    NotificationHistoryRepository notificationHistoryRepository) {
        this.announcementRepository = announcementRepository;
        this.userSavedAnnouncementRepository = userSavedAnnouncementRepository;
        this.notificationRepository = notificationRepository;
        this.notificationHistoryRepository = notificationHistoryRepository;
    }

    public int generateDeadlineNotifications(LocalDate today) {
        int created = 0;
        created += notifyAnnouncements(announcementRepository.findByApplicationStartDateAndDeletedFalseAndMergedFalse(today.plusDays(1)),
                NotificationType.START_MINUS_1, "접수 시작 하루 전");
        created += notifyAnnouncements(announcementRepository.findByApplicationEndDateAndDeletedFalseAndMergedFalse(today.plusDays(3)),
                NotificationType.END_MINUS_3, "마감 3일 전");
        created += notifyAnnouncements(announcementRepository.findByApplicationEndDateAndDeletedFalseAndMergedFalse(today.plusDays(1)),
                NotificationType.END_MINUS_1, "마감 하루 전");
        created += notifyAnnouncements(announcementRepository.findByApplicationEndDateAndDeletedFalseAndMergedFalse(today),
                NotificationType.END_DAY, "마감 당일");
        return created;
    }

    private int notifyAnnouncements(List<Announcement> announcements, NotificationType notificationType, String phaseText) {
        int created = 0;
        for (Announcement announcement : announcements) {
            List<UserSavedAnnouncement> savedAnnouncements = userSavedAnnouncementRepository.findByAnnouncementId(announcement.getId());
            for (UserSavedAnnouncement savedAnnouncement : savedAnnouncements) {
                Long userId = savedAnnouncement.getUser().getId();
                if (notificationHistoryRepository.existsByUserIdAndAnnouncementIdAndNotificationType(userId, announcement.getId(), notificationType)) {
                    continue;
                }
                notificationRepository.save(Notification.builder()
                        .user(savedAnnouncement.getUser())
                        .announcement(announcement)
                        .notificationType(notificationType)
                        .title(buildTitle(announcement, phaseText))
                        .message(buildMessage(announcement, phaseText))
                        .read(false)
                        .readAt(null)
                        .build());
                notificationHistoryRepository.save(NotificationHistory.builder()
                        .user(savedAnnouncement.getUser())
                        .announcement(announcement)
                        .notificationType(notificationType)
                        .sentAt(LocalDateTime.now())
                        .build());
                created++;
            }
        }
        return created;
    }

    private String buildTitle(Announcement announcement, String phaseText) {
        return "[공고 알림] " + phaseText;
    }

    private String buildMessage(Announcement announcement, String phaseText) {
        String endDate = announcement.getApplicationEndDate() == null ? "" : " (마감일: " + announcement.getApplicationEndDate() + ")";
        return announcement.getNoticeName() + " 공고가 " + phaseText + "입니다." + endDate;
    }
}
