package com.example.demo.announcement.controller;

import com.example.demo.announcement.dto.FilterOptionResponse;
import com.example.demo.announcement.service.AnnouncementQueryService;
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
    public FilterOptionResponse regions() {
        return announcementQueryService.regionLevel1Options();
    }

    @GetMapping("/supply-types")
    public FilterOptionResponse supplyTypes() {
        return announcementQueryService.supplyTypeOptions();
    }

    @GetMapping("/providers")
    public FilterOptionResponse providers() {
        return announcementQueryService.providerOptions();
    }
}
