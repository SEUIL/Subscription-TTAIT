package com.example.demo.announcement.controller;

import com.example.demo.announcement.dto.AnnouncementDetailResponse;
import com.example.demo.announcement.dto.AnnouncementListItemResponse;
import com.example.demo.announcement.service.AnnouncementQueryService;
import com.example.demo.common.web.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementQueryService announcementQueryService;

    public AnnouncementController(AnnouncementQueryService announcementQueryService) {
        this.announcementQueryService = announcementQueryService;
    }

    @GetMapping
    public Page<AnnouncementListItemResponse> getAnnouncements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String regionLevel1,
            @RequestParam(required = false) String regionLevel2,
            @RequestParam(required = false) String supplyType,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "recommended") String sort
    ) {
        Long userId;
        try {
            userId = CurrentUser.id();
        } catch (Exception ignored) {
            userId = null;
        }
        return announcementQueryService.getAnnouncements(
                userId,
                regionLevel1,
                regionLevel2,
                supplyType,
                provider,
                status,
                keyword,
                sort,
                PageRequest.of(page, size)
        );
    }

    @GetMapping("/{announcementId}")
    public AnnouncementDetailResponse getAnnouncementDetail(@PathVariable Long announcementId) {
        Long userId;
        try {
            userId = CurrentUser.id();
        } catch (Exception ignored) {
            userId = null;
        }
        return announcementQueryService.getAnnouncementDetail(announcementId, userId);
    }
}
