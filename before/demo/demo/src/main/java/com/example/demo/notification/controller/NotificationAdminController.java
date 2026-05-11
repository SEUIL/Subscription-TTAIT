package com.example.demo.notification.controller;

import com.example.demo.notification.service.NotificationBatchService;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/notifications")
public class NotificationAdminController {

    private final NotificationBatchService notificationBatchService;

    public NotificationAdminController(NotificationBatchService notificationBatchService) {
        this.notificationBatchService = notificationBatchService;
    }

    @PostMapping("/deadline/run")
    public Map<String, Object> runDeadlineNotifications(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate today) {
        LocalDate targetDate = today == null ? LocalDate.now() : today;
        int created = notificationBatchService.generateDeadlineNotifications(targetDate);
        return Map.of(
                "targetDate", targetDate,
                "createdCount", created
        );
    }
}
