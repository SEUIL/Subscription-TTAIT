package com.ttait.subscription.admin.controller;

import com.ttait.subscription.admin.dto.MarketSnapshotAggregateRequest;
import com.ttait.subscription.admin.dto.MarketSnapshotAggregateResponse;
import com.ttait.subscription.market.service.MarketPriceSnapshotAggregationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/market/snapshots")
public class AdminMarketSnapshotController {

    private final MarketPriceSnapshotAggregationService aggregationService;

    public AdminMarketSnapshotController(MarketPriceSnapshotAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @PostMapping("/aggregate")
    public ResponseEntity<MarketSnapshotAggregateResponse> aggregate(
            @RequestBody MarketSnapshotAggregateRequest request) {
        MarketPriceSnapshotAggregationService.AggregationResult result = aggregationService.aggregate(
                request.sourceType(),
                request.lawdCd(),
                request.dealYmFrom(),
                request.dealYmTo(),
                request.areaMin(),
                request.areaMax(),
                request.minimumSampleCount()
        );
        return ResponseEntity.ok(toResponse(result));
    }

    private MarketSnapshotAggregateResponse toResponse(
            MarketPriceSnapshotAggregationService.AggregationResult result) {
        return new MarketSnapshotAggregateResponse(
                result.snapshotId(),
                result.sourceType().name(),
                result.lawdCd(),
                result.dealYmFrom(),
                result.dealYmTo(),
                result.areaMin(),
                result.areaMax(),
                result.sampleCount(),
                result.avgDepositAmount(),
                result.medianDepositAmount(),
                result.avgMonthlyRentAmount(),
                result.medianMonthlyRentAmount(),
                result.avgTradeAmount(),
                result.medianTradeAmount(),
                result.status(),
                result.snapshotKey(),
                result.aggregatedAt()
        );
    }
}
