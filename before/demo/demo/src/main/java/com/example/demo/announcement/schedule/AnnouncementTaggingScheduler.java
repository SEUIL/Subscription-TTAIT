package com.example.demo.announcement.schedule;

import com.example.demo.announcement.service.AnnouncementTaggingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AnnouncementTaggingScheduler {

    private final AnnouncementTaggingService announcementTaggingService;

    public AnnouncementTaggingScheduler(AnnouncementTaggingService announcementTaggingService) {
        this.announcementTaggingService = announcementTaggingService;
    }

    @Scheduled(cron = "0 15 * * * *", zone = "Asia/Seoul")
    public void retagAll() {
        announcementTaggingService.retagAll();
    }
}
