package com.ttait.subscription.admin.controller;

import com.ttait.subscription.admin.dto.MarketReadinessResponse;
import com.ttait.subscription.admin.service.AdminMarketReadinessService;
import com.ttait.subscription.market.domain.MarketSourceType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Market Readiness", description = "공고별 주변시세 준비 상태 조회 API")
@RestController
@RequestMapping("/api/admin/market/announcements")
public class AdminMarketReadinessController {

    private final AdminMarketReadinessService readinessService;

    public AdminMarketReadinessController(AdminMarketReadinessService readinessService) {
        this.readinessService = readinessService;
    }

    @Operation(
            summary = "공고 주변시세 준비 상태 조회",
            description = "공고 unit별 lawdCd, 면적, RTMS raw, snapshot 준비 상태와 blocker를 조회합니다."
    )
    @GetMapping("/{announcementId}/readiness")
    public MarketReadinessResponse getReadiness(
            @Parameter(example = "1") @PathVariable Long announcementId,
            @Parameter(example = "APT_RENT") @RequestParam(defaultValue = "APT_RENT") MarketSourceType sourceType,
            @Parameter(example = "202401") @RequestParam String dealYmFrom,
            @Parameter(example = "202406") @RequestParam String dealYmTo) {
        return readinessService.getReadiness(announcementId, sourceType, dealYmFrom, dealYmTo);
    }
}
