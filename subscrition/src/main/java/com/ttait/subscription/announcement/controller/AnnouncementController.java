package com.ttait.subscription.announcement.controller;

import com.ttait.subscription.announcement.dto.AnnouncementDetailResponse;
import com.ttait.subscription.announcement.dto.AnnouncementListItemResponse;
import com.ttait.subscription.announcement.service.AnnouncementQueryService;
import com.ttait.subscription.auth.domain.AuthenticatedUser;
import com.ttait.subscription.user.domain.enums.CategoryCode;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
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
            @RequestParam(required = false) String regionLevel2,
            @RequestParam(required = false) String supplyType,
            @RequestParam(required = false) String houseType,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long minDeposit,
            @RequestParam(required = false) Long maxDeposit,
            @RequestParam(required = false) Long minMonthlyRent,
            @RequestParam(required = false) Long maxMonthlyRent,
            @RequestParam(required = false) List<CategoryCode> categories,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            Authentication authentication
    ) {
        Long userId = null;
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser principal) {
            userId = principal.userId();
        }
        return announcementQueryService.getAnnouncements(
                regionLevel1,
                regionLevel2,
                supplyType,
                houseType,
                provider,
                status,
                keyword,
                minDeposit,
                maxDeposit,
                minMonthlyRent,
                maxMonthlyRent,
                categories,
                PageRequest.of(page, size, resolveSort(sort)),
                userId
        );
    }

    private Sort resolveSort(String sort) {
        String value = sort == null ? "" : sort.trim();
        return switch (value) {
            case "latest" -> Sort.by(Sort.Direction.DESC, "announcementDate");
            case "deposit_asc" -> Sort.by(Sort.Direction.ASC, "depositAmount");
            case "deposit_desc" -> Sort.by(Sort.Direction.DESC, "depositAmount");
            default -> Sort.unsorted();
        };
    }

    @GetMapping("/{announcementId}")
    public AnnouncementDetailResponse getAnnouncementDetail(@PathVariable Long announcementId) {
        return announcementQueryService.getAnnouncementDetail(announcementId);
    }
}
