package com.ttait.subscription.external.controller;

import com.ttait.subscription.external.service.NoticeImportOrchestrator;
import com.ttait.subscription.external.service.NoticeImportOrchestrator.ImportResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/import")
@Tag(name = "Admin - Import", description = "공고 수집 관리자 API")
public class ImportController {

    private final NoticeImportOrchestrator orchestrator;

    public ImportController(NoticeImportOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/lh")
    @Operation(summary = "LH 공고 수집", description = "LH API에서 공고를 수집하고 PDF를 AI로 파싱하여 저장합니다.")
    public ResponseEntity<ImportResult> importLh(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        ImportResult result = orchestrator.importLhNotices(page, size);
        return ResponseEntity.ok(result);
    }
}
