package com.ttait.subscription.admin.controller;

import com.ttait.subscription.admin.dto.RtmsCollectionAllRequest;
import com.ttait.subscription.admin.dto.RtmsCollectionAllResponse;
import com.ttait.subscription.admin.dto.RtmsCollectionRequest;
import com.ttait.subscription.admin.dto.RtmsCollectionResponse;
import com.ttait.subscription.admin.service.AdminMarketCollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Market RTMS", description = "주변시세 RTMS 원천 데이터 수집 API")
@RestController
@RequestMapping("/api/admin/market")
public class AdminMarketCollectionController {

    private final AdminMarketCollectionService collectionService;

    public AdminMarketCollectionController(AdminMarketCollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @Operation(
            summary = "RTMS 단일 page 수집",
            description = "국토교통부 실거래가 API 한 page를 조회해 MarketTransactionRaw에 중복 없이 저장합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "sourceType": "APT_RENT",
                              "lawdCd": "28200",
                              "dealYm": "202405",
                              "pageNo": 1,
                              "numOfRows": 100
                            }
                            """))
            )
    )
    @PostMapping("/rtms/collect")
    public ResponseEntity<RtmsCollectionResponse> collectRtms(@RequestBody RtmsCollectionRequest request) {
        return ResponseEntity.ok(collectionService.collectRtms(request));
    }

    @Operation(
            summary = "RTMS 전체 page 수집",
            description = "totalCount 기준으로 여러 page를 순회하며 원천 데이터를 수집합니다. maxPages로 상한을 둘 수 있습니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "sourceType": "APT_RENT",
                              "lawdCd": "28200",
                              "dealYm": "202405",
                              "numOfRows": 100,
                              "maxPages": 10
                            }
                            """))
            )
    )
    @PostMapping("/rtms/collect-all")
    public ResponseEntity<RtmsCollectionAllResponse> collectAllRtms(@RequestBody RtmsCollectionAllRequest request) {
        return ResponseEntity.ok(collectionService.collectAllRtms(request));
    }

}
