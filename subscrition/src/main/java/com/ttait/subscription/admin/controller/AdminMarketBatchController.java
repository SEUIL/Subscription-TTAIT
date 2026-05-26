package com.ttait.subscription.admin.controller;

import com.ttait.subscription.admin.dto.MarketRtmsSnapshotBatchRequest;
import com.ttait.subscription.admin.dto.MarketRtmsSnapshotBatchResponse;
import com.ttait.subscription.admin.service.AdminMarketBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Market Batch", description = "주변시세 운영 배치 API")
@RestController
@RequestMapping("/api/admin/market/batches")
public class AdminMarketBatchController {

    private final AdminMarketBatchService batchService;

    public AdminMarketBatchController(AdminMarketBatchService batchService) {
        this.batchService = batchService;
    }

    @Operation(
            summary = "RTMS 전체 수집 후 snapshot 집계",
            description = "지정한 법정동/거래월의 RTMS 데이터를 전체 page 기준으로 수집한 뒤 같은 요청에서 snapshot을 재계산합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "sourceType": "APT_RENT",
                              "lawdCd": "28200",
                              "dealYm": "202405",
                              "numOfRows": 100,
                              "maxPages": 10,
                              "dealYmFrom": "202405",
                              "dealYmTo": "202405",
                              "areaMin": 35.0,
                              "areaMax": 85.0,
                              "minimumSampleCount": 3
                            }
                            """))
            )
    )
    @PostMapping("/rtms-snapshot")
    public ResponseEntity<MarketRtmsSnapshotBatchResponse> collectRtmsAndAggregateSnapshot(
            @RequestBody MarketRtmsSnapshotBatchRequest request) {
        return ResponseEntity.ok(batchService.collectRtmsAndAggregateSnapshot(request));
    }
}
