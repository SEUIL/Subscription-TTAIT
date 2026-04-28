package com.ttait.subscription.admin.controller;

import com.ttait.subscription.admin.dto.ManualAnnouncementRequest;
import com.ttait.subscription.admin.service.AdminAnnouncementService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 공고 직접 삭제 및 수동 등록 관리자 API
// ADMIN 권한 필요 — SecurityConfig에서 /api/admin/** 전체 보호
@RestController
@RequestMapping("/api/admin/announcements")
public class AdminAnnouncementController {

    private final AdminAnnouncementService adminAnnouncementService;

    public AdminAnnouncementController(AdminAnnouncementService adminAnnouncementService) {
        this.adminAnnouncementService = adminAnnouncementService;
    }

    // 공고 소프트 삭제 — 복구 가능 (deleted 플래그만 변경)
    @DeleteMapping("/{announcementId}")
    public ResponseEntity<Void> delete(@PathVariable Long announcementId) {
        adminAnnouncementService.delete(announcementId);
        return ResponseEntity.noContent().build();
    }

    // 수동 공고 등록 — 등록 후 즉시 검수 대기(PENDING) 상태로 투입
    // 응답: 201 Created + Location 헤더에 공고 조회 URL
    @PostMapping
    public ResponseEntity<Void> register(@Valid @RequestBody ManualAnnouncementRequest request) {
        Long id = adminAnnouncementService.register(request);
        return ResponseEntity.created(URI.create("/api/announcements/" + id)).build();
    }
}
