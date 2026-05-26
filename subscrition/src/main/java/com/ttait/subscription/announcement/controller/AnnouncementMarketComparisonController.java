package com.ttait.subscription.announcement.controller;

import com.ttait.subscription.market.domain.MarketSourceType;
import com.ttait.subscription.market.dto.MarketComparisonResponse;
import com.ttait.subscription.market.service.MarketComparisonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Market Comparison", description = "공고 단위 주변시세 비교 API")
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementMarketComparisonController {

    private final MarketComparisonService marketComparisonService;

    public AnnouncementMarketComparisonController(MarketComparisonService marketComparisonService) {
        this.marketComparisonService = marketComparisonService;
    }

    @Operation(
            summary = "공고 단위 주변시세 비교",
            description = "공고 단위의 보증금/월세와 사전에 집계된 주변시세 snapshot을 비교합니다. status는 COMPARABLE, INSUFFICIENT_DATA, SNAPSHOT_NOT_FOUND, UNIT_LAWD_CD_MISSING, UNIT_AREA_MISSING 등이 될 수 있습니다."
    )
    @GetMapping("/{announcementId}/units/{unitId}/market-comparison")
    public MarketComparisonResponse compareUnitMarketPrice(
            @Parameter(example = "1") @PathVariable Long announcementId,
            @Parameter(example = "3") @PathVariable Long unitId,
            @Parameter(example = "APT_RENT") @RequestParam MarketSourceType sourceType,
            @Parameter(example = "202405") @RequestParam String dealYmFrom,
            @Parameter(example = "202405") @RequestParam String dealYmTo) {
        return marketComparisonService.compare(announcementId, unitId, sourceType, dealYmFrom, dealYmTo);
    }
}
