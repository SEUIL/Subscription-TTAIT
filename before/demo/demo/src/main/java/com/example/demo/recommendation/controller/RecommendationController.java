package com.example.demo.recommendation.controller;

import com.example.demo.announcement.dto.AnnouncementListItemResponse;
import com.example.demo.announcement.service.AnnouncementQueryService;
import com.example.demo.common.web.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final AnnouncementQueryService announcementQueryService;

    public RecommendationController(AnnouncementQueryService announcementQueryService) {
        this.announcementQueryService = announcementQueryService;
    }

    @GetMapping
    public Page<AnnouncementListItemResponse> recommendations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return announcementQueryService.getAnnouncements(
                CurrentUser.id(),
                null,
                null,
                null,
                null,
                "OPEN",
                null,
                "recommended",
                PageRequest.of(page, size)
        );
    }
}
