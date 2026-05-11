package com.example.demo.notification.controller;

import com.example.demo.common.web.CurrentUser;
import com.example.demo.notification.service.SavedAnnouncementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/announcements")
public class SavedAnnouncementController {

    private final SavedAnnouncementService savedAnnouncementService;

    public SavedAnnouncementController(SavedAnnouncementService savedAnnouncementService) {
        this.savedAnnouncementService = savedAnnouncementService;
    }

    @PostMapping("/{announcementId}/save")
    public ResponseEntity<Void> save(@PathVariable Long announcementId) {
        savedAnnouncementService.save(CurrentUser.id(), announcementId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{announcementId}/save")
    public ResponseEntity<Void> unsave(@PathVariable Long announcementId) {
        savedAnnouncementService.unsave(CurrentUser.id(), announcementId);
        return ResponseEntity.noContent().build();
    }
}
