package com.example.demo.notification.schedule;

import com.example.demo.notification.service.NotificationBatchService;
import java.time.LocalDate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationScheduler {

    private final NotificationBatchService notificationBatchService;

    public NotificationScheduler(NotificationBatchService notificationBatchService) {
        this.notificationBatchService = notificationBatchService;
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void generateDeadlineNotifications() {
        notificationBatchService.generateDeadlineNotifications(LocalDate.now());
    }
}
