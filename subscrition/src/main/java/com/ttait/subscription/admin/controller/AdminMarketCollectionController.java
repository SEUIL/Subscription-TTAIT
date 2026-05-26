package com.ttait.subscription.admin.controller;

import com.ttait.subscription.admin.dto.RtmsCollectionAllRequest;
import com.ttait.subscription.admin.dto.RtmsCollectionAllResponse;
import com.ttait.subscription.admin.dto.RtmsCollectionRequest;
import com.ttait.subscription.admin.dto.RtmsCollectionResponse;
import com.ttait.subscription.admin.service.AdminMarketCollectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/market")
public class AdminMarketCollectionController {

    private final AdminMarketCollectionService collectionService;

    public AdminMarketCollectionController(AdminMarketCollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @PostMapping("/rtms/collect")
    public ResponseEntity<RtmsCollectionResponse> collectRtms(@RequestBody RtmsCollectionRequest request) {
        return ResponseEntity.ok(collectionService.collectRtms(request));
    }

    @PostMapping("/rtms/collect-all")
    public ResponseEntity<RtmsCollectionAllResponse> collectAllRtms(@RequestBody RtmsCollectionAllRequest request) {
        return ResponseEntity.ok(collectionService.collectAllRtms(request));
    }

}
