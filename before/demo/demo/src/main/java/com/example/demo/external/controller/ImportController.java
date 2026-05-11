package com.example.demo.external.controller;

import com.example.demo.external.service.NoticeImportOrchestrator;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/import")
public class ImportController {

    private final NoticeImportOrchestrator noticeImportOrchestrator;

    public ImportController(NoticeImportOrchestrator noticeImportOrchestrator) {
        this.noticeImportOrchestrator = noticeImportOrchestrator;
    }

    @PostMapping("/myhome/notices")
    public Map<String, Object> importMyHome(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        int imported = noticeImportOrchestrator.importMyHomeNotices(page, size);
        return Map.of("source", "MYHOME", "importedCount", imported);
    }

    @PostMapping("/lh/notices")
    public Map<String, Object> importLh(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        int imported = noticeImportOrchestrator.importLhNotices(page, size);
        return Map.of("source", "LH", "importedCount", imported);
    }

    @PostMapping("/lh/notices/{panId}/detail")
    public Map<String, Object> importLhDetail(@PathVariable String panId,
                                              @RequestParam String ccrCnntSysDsCd,
                                              @RequestParam String splInfTpCd) {
        noticeImportOrchestrator.importLhDetail(panId, ccrCnntSysDsCd, splInfTpCd);
        return Map.of("source", "LH", "panId", panId, "detailImported", true);
    }
}
