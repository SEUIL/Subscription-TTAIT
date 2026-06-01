package com.ttait.subscription.admin.controller;

import com.ttait.subscription.admin.dto.MarketPrepareRequest;
import com.ttait.subscription.admin.dto.MarketPrepareResponse;
import com.ttait.subscription.admin.service.AdminMarketPrepareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Market Prepare", description = "공고 단위 주변시세 준비 API")
@RestController
@RequestMapping("/api/admin/market/announcements")
public class AdminMarketPrepareController {

    private final AdminMarketPrepareService prepareService;

    public AdminMarketPrepareController(AdminMarketPrepareService prepareService) {
        this.prepareService = prepareService;
    }

    @Operation(
            summary = "공고 주변시세 데이터 준비",
            description = "공고 unit의 lawdCd/면적을 기준으로 RTMS 수집과 snapshot 집계를 동기 실행합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "sourceType": "APT_RENT",
                              "dealYm": "202406",
                              "dealYmFrom": "202401",
                              "dealYmTo": "202406",
                              "numOfRows": 100,
                              "maxPages": 10,
                              "minimumSampleCount": 3,
                              "retryNoLawdCode": true
                            }
                            """))
            )
    )
    @PostMapping("/{announcementId}/prepare")
    public ResponseEntity<MarketPrepareResponse> prepare(
            @PathVariable Long announcementId,
            @RequestBody MarketPrepareRequest request) {
        return ResponseEntity.ok(prepareService.prepare(announcementId, request));
    }
}
