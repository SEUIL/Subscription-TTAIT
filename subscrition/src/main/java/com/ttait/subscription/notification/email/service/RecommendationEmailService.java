package com.ttait.subscription.notification.email.service;

import com.ttait.subscription.announcement.dto.RecommendationItemResponse;
import com.ttait.subscription.announcement.service.RecommendationService;
import com.ttait.subscription.notification.email.domain.EmailNotificationLog;
import com.ttait.subscription.notification.email.domain.EmailNotificationType;
import com.ttait.subscription.notification.email.repository.EmailNotificationLogRepository;
import com.ttait.subscription.notification.email.sender.MessageSender;
import com.ttait.subscription.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class RecommendationEmailService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationEmailService.class);
    private static final int CANDIDATE_FETCH_SIZE = 20;
    private static final int MAX_EMAIL_ITEMS = 10;

    private final RecommendationService recommendationService;
    private final EmailNotificationLogRepository logRepository;
    private final MessageSender messageSender;
    private final TemplateEngine templateEngine;

    public RecommendationEmailService(RecommendationService recommendationService,
                                      EmailNotificationLogRepository logRepository,
                                      MessageSender messageSender,
                                      TemplateEngine templateEngine) {
        this.recommendationService = recommendationService;
        this.logRepository = logRepository;
        this.messageSender = messageSender;
        this.templateEngine = templateEngine;
    }

    public void sendFor(User user) {
        List<RecommendationItemResponse> candidates;
        try {
            candidates = recommendationService
                .getRecommendations(user.getId(), PageRequest.of(0, CANDIDATE_FETCH_SIZE))
                .getContent();
        } catch (Exception e) {
            log.warn("Skipping recommendation email for userId={}: {}", user.getId(), e.getMessage());
            return;
        }

        if (candidates.isEmpty()) {
            return;
        }

        List<Long> candidateIds = candidates.stream().map(RecommendationItemResponse::announcementId).toList();
        Set<Long> alreadySent = logRepository.findAlreadySentAnnouncementIds(
            user.getId(), EmailNotificationType.RECOMMENDATION, candidateIds);

        List<RecommendationItemResponse> newItems = candidates.stream()
            .filter(item -> !alreadySent.contains(item.announcementId()))
            .limit(MAX_EMAIL_ITEMS)
            .toList();

        if (newItems.isEmpty()) {
            return;
        }

        Context ctx = new Context();
        ctx.setVariable("userName", user.getLoginId());
        ctx.setVariable("items", newItems);
        String html = templateEngine.process("email/recommendation", ctx);

        LocalDateTime now = LocalDateTime.now();
        boolean success = false;
        String errorMessage = null;
        try {
            messageSender.send(user.getEmail(), "[청약따잇] 오늘의 맞춤 공고가 도착했어요", html);
            success = true;
        } catch (Exception e) {
            errorMessage = truncate(e.getMessage(), 500);
            log.error("Failed to send recommendation email to userId={}: {}", user.getId(), e.getMessage());
        }

        boolean finalSuccess = success;
        String finalErrorMessage = errorMessage;
        List<EmailNotificationLog> logs = newItems.stream()
            .map(item -> EmailNotificationLog.builder()
                .userId(user.getId())
                .announcementId(item.announcementId())
                .type(EmailNotificationType.RECOMMENDATION)
                .sentAt(now)
                .success(finalSuccess)
                .errorMessage(finalErrorMessage)
                .build())
            .toList();
        logRepository.saveAll(logs);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
