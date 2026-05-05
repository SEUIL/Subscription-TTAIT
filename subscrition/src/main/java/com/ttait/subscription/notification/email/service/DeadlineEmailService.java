package com.ttait.subscription.notification.email.service;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.dto.RecommendationItemResponse;
import com.ttait.subscription.notification.email.domain.EmailNotificationLog;
import com.ttait.subscription.notification.email.domain.EmailNotificationType;
import com.ttait.subscription.notification.email.repository.EmailNotificationLogRepository;
import com.ttait.subscription.notification.email.sender.MessageSender;
import com.ttait.subscription.notification.favorite.domain.UserFavoriteAnnouncement;
import com.ttait.subscription.notification.favorite.repository.UserFavoriteAnnouncementRepository;
import com.ttait.subscription.user.domain.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class DeadlineEmailService {

    private static final Logger log = LoggerFactory.getLogger(DeadlineEmailService.class);

    private static final Map<Integer, EmailNotificationType> DEADLINE_DAYS = Map.of(
        7, EmailNotificationType.DEADLINE_7,
        3, EmailNotificationType.DEADLINE_3,
        1, EmailNotificationType.DEADLINE_1
    );

    private final UserFavoriteAnnouncementRepository favoriteRepository;
    private final EmailNotificationLogRepository logRepository;
    private final MessageSender messageSender;
    private final TemplateEngine templateEngine;

    public DeadlineEmailService(UserFavoriteAnnouncementRepository favoriteRepository,
                                EmailNotificationLogRepository logRepository,
                                MessageSender messageSender,
                                TemplateEngine templateEngine) {
        this.favoriteRepository = favoriteRepository;
        this.logRepository = logRepository;
        this.messageSender = messageSender;
        this.templateEngine = templateEngine;
    }

    public void sendFor(User user, LocalDate today) {
        List<UserFavoriteAnnouncement> favorites = favoriteRepository.findActiveByUserId(user.getId());
        if (favorites.isEmpty()) {
            return;
        }

        for (UserFavoriteAnnouncement favorite : favorites) {
            Announcement announcement = favorite.getAnnouncement();
            LocalDate endDate = announcement.getApplicationEndDate();
            if (endDate == null) continue;

            for (Map.Entry<Integer, EmailNotificationType> entry : DEADLINE_DAYS.entrySet()) {
                int days = entry.getKey();
                EmailNotificationType type = entry.getValue();

                if (!today.equals(endDate.minusDays(days))) continue;
                if (logRepository.existsByUserIdAndAnnouncementIdAndType(user.getId(), announcement.getId(), type)) continue;

                sendDeadlineEmail(user, announcement, "D-" + days, type);
            }
        }
    }

    private void sendDeadlineEmail(User user, Announcement announcement, String dDayLabel,
                                   EmailNotificationType type) {
        RecommendationItemResponse announcementVm = toViewModel(announcement);
        Context ctx = new Context();
        ctx.setVariable("userName", user.getLoginId());
        ctx.setVariable("dDayLabel", dDayLabel);
        ctx.setVariable("announcement", announcementVm);
        String html = templateEngine.process("email/deadline-reminder", ctx);

        LocalDateTime now = LocalDateTime.now();
        boolean success = false;
        String errorMessage = null;
        try {
            messageSender.send(user.getEmail(),
                "[청약따잇] " + dDayLabel + " 즐겨찾기 공고 마감 임박", html);
            success = true;
        } catch (Exception e) {
            errorMessage = e.getMessage() != null && e.getMessage().length() > 500
                ? e.getMessage().substring(0, 500) : e.getMessage();
            log.error("Failed to send deadline email to userId={} type={}: {}", user.getId(), type, e.getMessage());
        }

        logRepository.save(EmailNotificationLog.builder()
            .userId(user.getId())
            .announcementId(announcement.getId())
            .type(type)
            .sentAt(now)
            .success(success)
            .errorMessage(errorMessage)
            .build());
    }

    private RecommendationItemResponse toViewModel(Announcement a) {
        return new RecommendationItemResponse(
            a.getId(), a.getNoticeName(), a.getProviderName(),
            a.getSupplyTypeNormalized(), a.getHouseTypeNormalized(),
            a.getRegionLevel1(), a.getRegionLevel2(), a.getFullAddress(), a.getComplexName(),
            a.getDepositAmount(), a.getMonthlyRentAmount(),
            a.getApplicationStartDate(), a.getApplicationEndDate(),
            a.getNoticeStatus().name(), a.getSourceNoticeUrl(),
            0, List.of()
        );
    }
}
