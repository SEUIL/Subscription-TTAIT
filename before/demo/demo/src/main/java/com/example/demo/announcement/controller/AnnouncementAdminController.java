package com.example.demo.announcement.controller;

import com.example.demo.announcement.service.AnnouncementTaggingService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/announcements")
public class AnnouncementAdminController {

    private final AnnouncementTaggingService announcementTaggingService;

    public AnnouncementAdminController(AnnouncementTaggingService announcementTaggingService) {
        this.announcementTaggingService = announcementTaggingService;
    }

    @PostMapping("/retag")
    public Map<String, Object> retagAll() {
        int processed = announcementTaggingService.retagAll();
        return Map.of("processedCount", processed);
    }
}
