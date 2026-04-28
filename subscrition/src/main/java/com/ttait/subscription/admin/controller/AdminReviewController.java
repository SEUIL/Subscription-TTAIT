package com.ttait.subscription.admin.controller;

import com.ttait.subscription.admin.dto.AdminReviewDetailResponse;
import com.ttait.subscription.admin.dto.AdminReviewListResponse;
import com.ttait.subscription.admin.dto.AdminReviewRequest;
import com.ttait.subscription.admin.dto.AdminStatsResponse;
import com.ttait.subscription.admin.service.AdminReviewService;
import com.ttait.subscription.announcement.domain.ParseReviewStatus;
import com.ttait.subscription.auth.domain.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// AI 파싱 검수 관리자 API
// ADMIN 권한 필요 — SecurityConfig에서 /api/admin/** 전체 보호
@RestController
@RequestMapping("/api/admin/review")
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    public AdminReviewController(AdminReviewService adminReviewService) {
        this.adminReviewService = adminReviewService;
    }

    // 검수 통계 대시보드 — 상태별 건수 + 전체 공고 수 + 오늘 처리된 건수
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> stats() {
        return ResponseEntity.ok(adminReviewService.getStats());
    }

    // 검수 상태별 공고 목록 조회 (기본: PENDING — 아직 검수 안 된 항목)
    // ?status=PENDING|APPROVED|CORRECTED|REJECTED|RE_IMPORT 로 필터링
    @GetMapping
    public ResponseEntity<Page<AdminReviewListResponse>> list(
            @RequestParam(defaultValue = "PENDING") ParseReviewStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminReviewService.listByStatus(status, PageRequest.of(page, size)));
    }

    // 특정 공고의 AI 파싱 결과 + 원문 텍스트를 함께 반환
    // 관리자가 AI가 추출한 값과 PDF 원문을 나란히 비교해 검수할 수 있도록 설계
    @GetMapping("/{announcementId}")
    public ResponseEntity<AdminReviewDetailResponse> detail(@PathVariable Long announcementId) {
        return ResponseEntity.ok(adminReviewService.getDetail(announcementId));
    }

    // 검수 액션 처리
    // action: APPROVE(AI 결과 그대로 확정) / CORRECT(값 수정 후 확정) / REJECT(폐기) / REIMPORT(재수집+재파싱)
    @PostMapping("/{announcementId}")
    public ResponseEntity<Void> review(@PathVariable Long announcementId,
                                       @RequestBody AdminReviewRequest request,
                                       @AuthenticationPrincipal AuthenticatedUser currentUser) {
        adminReviewService.review(announcementId, request, currentUser.loginId());
        return ResponseEntity.ok().build();
    }
}
