package com.ttait.subscription.announcement.controller;

import com.ttait.subscription.announcement.dto.AnnouncementDetailResponse;
import com.ttait.subscription.announcement.dto.AnnouncementListItemResponse;
import com.ttait.subscription.announcement.service.AnnouncementQueryService;
import com.ttait.subscription.user.domain.enums.CategoryCode;
import java.util.List;
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
            @RequestParam(required = false) String regionLevel1,
            @RequestParam(required = false) String supplyType,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) List<CategoryCode> categories,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return announcementQueryService.getAnnouncements(
                regionLevel1,
                supplyType,
                provider,
                status,
                categories,
                PageRequest.of(page, size)
        );
    }

    @GetMapping("/{announcementId}")
    public AnnouncementDetailResponse getAnnouncementDetail(@PathVariable Long announcementId) {
        return announcementQueryService.getAnnouncementDetail(announcementId);
    }
}
