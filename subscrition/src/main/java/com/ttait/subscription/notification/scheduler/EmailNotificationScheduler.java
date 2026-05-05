package com.ttait.subscription.notification.scheduler;

import com.ttait.subscription.notification.email.service.DeadlineEmailService;
import com.ttait.subscription.notification.email.service.RecommendationEmailService;
import com.ttait.subscription.user.domain.User;
import com.ttait.subscription.user.domain.enums.UserStatus;
import com.ttait.subscription.user.repository.UserRepository;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationScheduler.class);
    private static final int CHUNK_SIZE = 100;

    private final UserRepository userRepository;
    private final RecommendationEmailService recommendationEmailService;
    private final DeadlineEmailService deadlineEmailService;

    public EmailNotificationScheduler(UserRepository userRepository,
                                      RecommendationEmailService recommendationEmailService,
                                      DeadlineEmailService deadlineEmailService) {
        this.userRepository = userRepository;
        this.recommendationEmailService = recommendationEmailService;
        this.deadlineEmailService = deadlineEmailService;
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void sendDailyRecommendations() {
        log.info("Starting daily recommendation email batch");
        int sent = 0, failed = 0;
        int page = 0;

        while (true) {
            Page<User> users = userRepository.findByProfileCompletedTrueAndStatusAndDeletedFalse(
                UserStatus.ACTIVE, PageRequest.of(page, CHUNK_SIZE));

            for (User user : users.getContent()) {
                try {
                    recommendationEmailService.sendFor(user);
                    sent++;
                } catch (Exception e) {
                    failed++;
                    log.error("Recommendation email failed for userId={}: {}", user.getId(), e.getMessage());
                }
            }

            if (!users.hasNext()) break;
            page++;
        }

        log.info("Daily recommendation email batch done: sent={}, failed={}", sent, failed);
    }

    @Scheduled(cron = "0 5 9 * * *")
    public void sendDeadlineReminders() {
        log.info("Starting deadline reminder email batch");
        LocalDate today = LocalDate.now();
        int processed = 0, failed = 0;
        int page = 0;

        while (true) {
            Page<User> users = userRepository.findByProfileCompletedTrueAndStatusAndDeletedFalse(
                UserStatus.ACTIVE, PageRequest.of(page, CHUNK_SIZE));

            for (User user : users.getContent()) {
                try {
                    deadlineEmailService.sendFor(user, today);
                    processed++;
                } catch (Exception e) {
                    failed++;
                    log.error("Deadline reminder failed for userId={}: {}", user.getId(), e.getMessage());
                }
            }

            if (!users.hasNext()) break;
            page++;
        }

        log.info("Deadline reminder batch done: processed={}, failed={}", processed, failed);
    }
}
