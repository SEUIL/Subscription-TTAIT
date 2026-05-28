package com.ttait.subscription.admin.controller;

import com.ttait.subscription.admin.dto.AddressNormalizationRequest;
import com.ttait.subscription.admin.dto.AddressNormalizationResponse;
import com.ttait.subscription.admin.dto.LawdCodeMappingSeedRequest;
import com.ttait.subscription.admin.dto.LawdCodeMappingSeedResponse;
import com.ttait.subscription.admin.dto.LawdCodeMappingUpsertRequest;
import com.ttait.subscription.admin.dto.LawdCodeMappingUpsertResponse;
import com.ttait.subscription.admin.service.AdminMarketAddressService;
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

@Tag(name = "Admin - Market Address", description = "주변시세 법정동 매핑 관리자 API")
@RestController
@RequestMapping("/api/admin/market/address")
public class AdminMarketAddressController {

    private final AdminMarketAddressService addressService;

    public AdminMarketAddressController(AdminMarketAddressService addressService) {
        this.addressService = addressService;
    }

    @Operation(
            summary = "법정동 코드 매핑 수동 upsert",
            description = "regionLevel2/legalDongName 기준 주소 정규화에 사용할 법정동 코드 매핑을 추가하거나 갱신합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "mappings": [
                                {
                                  "regionLevel1": "인천광역시",
                                  "regionLevel2": "부평구",
                                  "legalDongName": "부평동",
                                  "legalDongCode": "2823710100",
                                  "active": true
                                }
                              ]
                            }
                            """))
            )
    )
    @PostMapping("/lawd-code-mappings")
    public ResponseEntity<LawdCodeMappingUpsertResponse> upsertLawdCodeMappings(
            @RequestBody LawdCodeMappingUpsertRequest request) {
        return ResponseEntity.ok(addressService.upsertLawdCodeMappings(request));
    }

    @Operation(
            summary = "공공 법정동 코드 seed import",
            description = "법정동코드, 법정동명, 폐지여부 컬럼을 가진 CSV/TSV 원문을 읽어 매핑 테이블에 일괄 반영합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "delimiter": "tab",
                              "activeOnly": true,
                              "content": "법정동코드\\t법정동명\\t폐지여부\\n2823710100\\t인천광역시 부평구 부평동\\t존재"
                            }
                            """))
            )
    )
    @PostMapping("/lawd-code-mappings/import")
    public ResponseEntity<LawdCodeMappingSeedResponse> importLawdCodeMappings(
            @RequestBody LawdCodeMappingSeedRequest request) {
        return ResponseEntity.ok(addressService.importLawdCodeMappings(request));
    }

    @Operation(
            summary = "공고 단위 주소 법정동 재정규화",
            description = "LH import 이후 NOT_REQUESTED 또는 선택적으로 NO_LAWD_CODE 상태인 단위 주소에 법정동 코드를 다시 매핑합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = false,
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "retryNoLawdCode": true
                            }
                            """))
            )
    )
    @PostMapping("/announcements/{announcementId}/normalize-units")
    public ResponseEntity<AddressNormalizationResponse> normalizeAnnouncementUnits(
            @PathVariable Long announcementId,
            @RequestBody(required = false) AddressNormalizationRequest request) {
        boolean retryNoLawdCode = request != null && request.retryNoLawdCodeOrDefault();
        return ResponseEntity.ok(addressService.normalizeAnnouncementUnits(announcementId, retryNoLawdCode));
    }
}
