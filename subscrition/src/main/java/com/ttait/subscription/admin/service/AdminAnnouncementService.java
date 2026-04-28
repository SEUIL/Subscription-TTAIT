package com.ttait.subscription.admin.service;

import com.ttait.subscription.admin.dto.ManualAnnouncementRequest;
import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementDetail;
import com.ttait.subscription.announcement.domain.AnnouncementEligibility;
import com.ttait.subscription.announcement.domain.SourceType;
import com.ttait.subscription.announcement.repository.AnnouncementDetailRepository;
import com.ttait.subscription.announcement.repository.AnnouncementEligibilityRepository;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.common.exception.ApiException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 공고 직접 삭제 및 수동 등록 서비스
@Service
@Transactional
public class AdminAnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementDetailRepository announcementDetailRepository;
    private final AnnouncementEligibilityRepository eligibilityRepository;

    public AdminAnnouncementService(AnnouncementRepository announcementRepository,
                                    AnnouncementDetailRepository announcementDetailRepository,
                                    AnnouncementEligibilityRepository eligibilityRepository) {
        this.announcementRepository = announcementRepository;
        this.announcementDetailRepository = announcementDetailRepository;
        this.eligibilityRepository = eligibilityRepository;
    }

    // 공고 소프트 삭제 — deleted=true로 마킹, 연관된 detail도 함께 삭제
    // AnnouncementEligibility는 soft delete 미지원이나 findByReviewStatus 쿼리에서 deleted 공고 제외됨
    public void delete(Long announcementId) {
        Announcement announcement = announcementRepository.findByIdAndDeletedFalse(announcementId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "announcement not found"));
        announcement.softDelete();

        announcementDetailRepository.findByAnnouncementIdAndDeletedFalse(announcementId)
                .ifPresent(AnnouncementDetail::softDelete);
    }

    // 수동 공고 등록 — SourceType.MANUAL, UUID 기반 sourceNoticeId 자동 생성
    // 등록 후 AnnouncementEligibility를 PENDING 상태로 함께 생성 (검수 대기 큐에 바로 투입)
    public Long register(ManualAnnouncementRequest request) {
        String uid = UUID.randomUUID().toString().replace("-", "");
        String sourceNoticeId = "MANUAL_" + uid;

        Announcement announcement = announcementRepository.save(Announcement.builder()
                .sourcePrimary(SourceType.MANUAL)
                .sourceNoticeId(sourceNoticeId)
                .noticeName(request.noticeName())
                .providerName(request.providerName())
                .sourceNoticeUrl("")  // 수동 등록은 원본 URL 없음
                .noticeStatus(request.noticeStatus())
                .regionLevel1(request.regionLevel1())
                .regionLevel2(request.regionLevel2())
                .fullAddress(request.fullAddress())
                .complexName(request.complexName())
                .depositAmount(request.depositAmount())
                .monthlyRentAmount(request.monthlyRentAmount())
                .supplyHouseholdCount(request.supplyHouseholdCount())
                .applicationStartDate(request.applicationStartDate())
                .applicationEndDate(request.applicationEndDate())
                .matchKey(sourceNoticeId)  // MANUAL 등록은 자체가 고유 키
                .merged(false)
                .collectedAt(LocalDateTime.now())
                .build());

        eligibilityRepository.save(AnnouncementEligibility.builder()
                .announcement(announcement)
                .build());

        return announcement.getId();
    }
}
