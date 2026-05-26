package com.ttait.subscription.announcement.controller;

import com.ttait.subscription.market.domain.MarketSourceType;
import com.ttait.subscription.market.dto.MarketComparisonResponse;
import com.ttait.subscription.market.service.MarketComparisonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementMarketComparisonController {

    private final MarketComparisonService marketComparisonService;

    public AnnouncementMarketComparisonController(MarketComparisonService marketComparisonService) {
        this.marketComparisonService = marketComparisonService;
    }

    @GetMapping("/{announcementId}/units/{unitId}/market-comparison")
    public MarketComparisonResponse compareUnitMarketPrice(
            @PathVariable Long announcementId,
            @PathVariable Long unitId,
            @RequestParam MarketSourceType sourceType,
            @RequestParam String dealYmFrom,
            @RequestParam String dealYmTo) {
        return marketComparisonService.compare(announcementId, unitId, sourceType, dealYmFrom, dealYmTo);
    }
}
