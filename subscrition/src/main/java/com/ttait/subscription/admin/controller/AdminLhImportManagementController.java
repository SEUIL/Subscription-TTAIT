package com.ttait.subscription.admin.controller;

import com.ttait.subscription.admin.dto.LhCandidateCollectionResponse;
import com.ttait.subscription.admin.dto.LhImportCandidateListResponse;
import com.ttait.subscription.admin.dto.LhImportRunResult;
import com.ttait.subscription.admin.dto.LhSelectedImportRequest;
import com.ttait.subscription.admin.service.AdminLhImportManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/import/lh")
public class AdminLhImportManagementController {

    private final AdminLhImportManagementService importManagementService;

    public AdminLhImportManagementController(AdminLhImportManagementService importManagementService) {
        this.importManagementService = importManagementService;
    }

    @PostMapping("/candidates/collect")
    public ResponseEntity<LhCandidateCollectionResponse> collectCandidates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(importManagementService.collectCandidates(page, size));
    }

    @GetMapping("/candidates")
    public ResponseEntity<LhImportCandidateListResponse> listCandidates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(importManagementService.listCandidates(page, size, status));
    }

    @PostMapping("/selected")
    public ResponseEntity<LhImportRunResult> importSelected(@RequestBody LhSelectedImportRequest request) {
        return ResponseEntity.ok(importManagementService.importSelected(request));
    }

    @PostMapping("/{announcementId}/force-reparse")
    public ResponseEntity<LhImportRunResult> forceReparse(@PathVariable Long announcementId) {
        return ResponseEntity.ok(importManagementService.forceReparse(announcementId));
    }
}
