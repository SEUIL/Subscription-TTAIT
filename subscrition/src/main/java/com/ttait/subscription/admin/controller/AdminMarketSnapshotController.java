package com.ttait.subscription.admin.controller;

import com.ttait.subscription.admin.dto.MarketSnapshotAggregateRequest;
import com.ttait.subscription.admin.dto.MarketSnapshotAggregateResponse;
import com.ttait.subscription.market.service.MarketPriceSnapshotAggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Market Snapshot", description = "주변시세 snapshot 집계 API")
@RestController
@RequestMapping("/api/admin/market/snapshots")
public class AdminMarketSnapshotController {

    private final MarketPriceSnapshotAggregationService aggregationService;

    public AdminMarketSnapshotController(MarketPriceSnapshotAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @Operation(
            summary = "주변시세 snapshot 집계",
            description = "수집된 RTMS 원천 데이터에서 법정동, 거래월 범위, 전용면적 범위별 평균/중앙값 snapshot을 생성합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "sourceType": "APT_RENT",
                              "lawdCd": "28200",
                              "dealYmFrom": "202405",
                              "dealYmTo": "202405",
                              "areaMin": 35.0,
                              "areaMax": 85.0,
                              "minimumSampleCount": 3
                            }
                            """))
            )
    )
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
