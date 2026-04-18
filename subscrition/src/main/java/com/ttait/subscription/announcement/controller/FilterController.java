package com.ttait.subscription.announcement.controller;

import com.ttait.subscription.announcement.dto.CategoryFilterOptionResponse;
import com.ttait.subscription.announcement.dto.FilterOptionResponse;
import com.ttait.subscription.announcement.service.AnnouncementQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/filters")
public class FilterController {

    private final AnnouncementQueryService announcementQueryService;

    public FilterController(AnnouncementQueryService announcementQueryService) {
        this.announcementQueryService = announcementQueryService;
    }

    @GetMapping("/regions")
    public FilterOptionResponse regionLevel1Options() {
        return announcementQueryService.regionLevel1Options();
    }

    @GetMapping("/supply-types")
    public FilterOptionResponse supplyTypeOptions() {
        return announcementQueryService.supplyTypeOptions();
    }

    @GetMapping("/providers")
    public FilterOptionResponse providerOptions() {
        return announcementQueryService.providerOptions();
    }

    @GetMapping("/categories")
    public CategoryFilterOptionResponse categoryOptions() {
        return announcementQueryService.categoryOptions();
    }
}
