package com.ttait.subscription.admin.service;

import com.ttait.subscription.admin.dto.AdminReviewDetailResponse;
import com.ttait.subscription.admin.dto.AdminReviewListResponse;
import com.ttait.subscription.admin.dto.AdminReviewRequest;
import com.ttait.subscription.admin.dto.AdminStatsResponse;
import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementDetail;
import com.ttait.subscription.announcement.domain.AnnouncementEligibility;
import com.ttait.subscription.announcement.domain.MaritalTargetType;
import com.ttait.subscription.announcement.domain.ParseReviewStatus;
import com.ttait.subscription.announcement.repository.AnnouncementDetailRepository;
import com.ttait.subscription.announcement.repository.AnnouncementEligibilityRepository;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.external.service.NoticeImportOrchestrator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// AI 파싱 결과 검수 서비스
// LH 공고 수집 후 OpenAI가 파싱한 자격조건을 관리자가 검토·확정하는 플로우를 담당
@Service
@Transactional
public class AdminReviewService {

    private static final Logger log = LoggerFactory.getLogger(AdminReviewService.class);

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementDetailRepository announcementDetailRepository;
    private final AnnouncementEligibilityRepository eligibilityRepository;
    private final NoticeImportOrchestrator orchestrator;

    public AdminReviewService(AnnouncementRepository announcementRepository,
                              AnnouncementDetailRepository announcementDetailRepository,
                              AnnouncementEligibilityRepository eligibilityRepository,
                              NoticeImportOrchestrator orchestrator) {
        this.announcementRepository = announcementRepository;
        this.announcementDetailRepository = announcementDetailRepository;
        this.eligibilityRepository = eligibilityRepository;
        this.orchestrator = orchestrator;
    }

    // 검수 상태별 공고 목록 페이징 조회 (읽기 전용 트랜잭션으로 성능 최적화)
    @Transactional(readOnly = true)
    public Page<AdminReviewListResponse> listByStatus(ParseReviewStatus status, Pageable pageable) {
        return eligibilityRepository.findByReviewStatus(status, pageable)
                .map(AdminReviewListResponse::from);
    }

    // AI 파싱 결과(eligibility) + 원문(detail) + 공고 기본정보를 합쳐서 반환
    // detail이 null일 수 있음 — PDF 파싱 실패 시 AnnouncementDetail이 저장 안 된 경우
    @Transactional(readOnly = true)
    public AdminReviewDetailResponse getDetail(Long announcementId) {
        Announcement announcement = announcementRepository.findByIdAndDeletedFalse(announcementId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "announcement not found"));
        AnnouncementEligibility eligibility = eligibilityRepository.findByAnnouncementId(announcementId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "eligibility not found for announcement " + announcementId));
        AnnouncementDetail detail = announcementDetailRepository.findByAnnouncementIdAndDeletedFalse(announcementId)
                .orElse(null);
        return AdminReviewDetailResponse.from(announcement, detail, eligibility);
    }

    // 검수 액션 분기 처리
    // toUpperCase() — 클라이언트가 소문자로 보내도 허용
    public void review(Long announcementId, AdminReviewRequest request, String reviewerLoginId) {
        String action = request.action();
        if (action == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "action is required");
        }

        switch (action.toUpperCase()) {
            case "APPROVE" -> handleApprove(announcementId, reviewerLoginId);
            case "CORRECT" -> handleCorrect(announcementId, request, reviewerLoginId);
            case "REJECT" -> handleReject(announcementId, request, reviewerLoginId);
            case "REIMPORT" -> handleReimport(announcementId, reviewerLoginId);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "unknown action: " + action);
        }
    }

    // AI 파싱 결과가 정확하다고 판단 → 그대로 APPROVED 확정
    private void handleApprove(Long announcementId, String reviewerLoginId) {
        AnnouncementEligibility eligibility = findEligibility(announcementId);
        eligibility.approve(reviewerLoginId);
    }

    // AI 파싱 오류 수정 후 확정
    // corrections 필드 null 허용 — 수정할 항목만 보내면 됨 (나머지는 기존 값 유지)
    // Announcement의 depositAmount/monthlyRentAmount도 같이 수정 (두 테이블 동기화 필요)
    private void handleCorrect(Long announcementId, AdminReviewRequest request, String reviewerLoginId) {
        AdminReviewRequest.Corrections c = request.corrections();
        if (c == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "corrections required for CORRECT action");
        }

        Announcement announcement = announcementRepository.findByIdAndDeletedFalse(announcementId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "announcement not found"));
        announcement.updateDepositAndRent(c.depositAmount(), c.monthlyRentAmount());
        if (c.supplyHouseholdCount() != null) {
            announcement.updateSupplyHouseholdCount(c.supplyHouseholdCount());
        }

        MaritalTargetType maritalTargetType = null;
        if (c.maritalTargetType() != null) {
            try {
                maritalTargetType = MaritalTargetType.valueOf(c.maritalTargetType());
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "unknown maritalTargetType: " + c.maritalTargetType());
            }
        }

        AnnouncementEligibility eligibility = findEligibility(announcementId);
        eligibility.correct(reviewerLoginId, request.note(),
                c.ageMin(), c.ageMax(), maritalTargetType,
                c.marriageYearLimit(), c.childrenMinCount(),
                c.homelessRequired(), c.lowIncomeRequired(),
                c.elderlyRequired(), c.elderlyAgeMin());
    }

    // AI 파싱 품질이 너무 낮거나 공고 자체가 부적절 → REJECTED 상태로 폐기
    private void handleReject(Long announcementId, AdminReviewRequest request, String reviewerLoginId) {
        AnnouncementEligibility eligibility = findEligibility(announcementId);
        eligibility.reject(reviewerLoginId, request.note());
    }

    // LH API + PDF 재수집·재파싱 후 검수 상태를 PENDING으로 초기화
    // orchestrator가 기존 데이터를 덮어쓰므로 eligibility.resetToPending()으로 검수 정보도 초기화
    private void handleReimport(Long announcementId, String reviewerLoginId) {
        log.info("Reimport triggered by {} for announcementId={}", reviewerLoginId, announcementId);
        orchestrator.reimportAnnouncement(announcementId);
        AnnouncementEligibility eligibility = findEligibility(announcementId);
        eligibility.resetToPending();
    }

    // ParseReviewStatus별 건수 + 전체 공고 수 + 오늘 처리된 건수 집계
    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        return new AdminStatsResponse(
                eligibilityRepository.countByReviewStatus(ParseReviewStatus.PENDING),
                eligibilityRepository.countByReviewStatus(ParseReviewStatus.APPROVED),
                eligibilityRepository.countByReviewStatus(ParseReviewStatus.CORRECTED),
                eligibilityRepository.countByReviewStatus(ParseReviewStatus.REJECTED),
                eligibilityRepository.countByReviewStatus(ParseReviewStatus.RE_IMPORT),
                announcementRepository.countByDeletedFalseAndMergedFalse(),
                eligibilityRepository.countByReviewedAtGreaterThanEqualAndReviewStatusNot(startOfToday, ParseReviewStatus.PENDING)
        );
    }

    private AnnouncementEligibility findEligibility(Long announcementId) {
        return eligibilityRepository.findByAnnouncementId(announcementId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "eligibility not found for announcement " + announcementId));
    }
}
