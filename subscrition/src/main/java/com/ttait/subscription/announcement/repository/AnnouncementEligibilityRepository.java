package com.ttait.subscription.announcement.repository;

import com.ttait.subscription.announcement.domain.AnnouncementEligibility;
import com.ttait.subscription.announcement.domain.ParseReviewStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementEligibilityRepository extends JpaRepository<AnnouncementEligibility, Long> {

    Optional<AnnouncementEligibility> findByAnnouncementId(Long announcementId);

    // 삭제된 공고는 검수 목록에서 제외
    @Query("SELECT e FROM AnnouncementEligibility e WHERE e.reviewStatus = :status AND e.announcement.deleted = false")
    Page<AnnouncementEligibility> findByReviewStatus(@Param("status") ParseReviewStatus status, Pageable pageable);

    // 삭제된 공고 제외
    @Query("SELECT COUNT(e) FROM AnnouncementEligibility e WHERE e.reviewStatus = :status AND e.announcement.deleted = false")
    long countByReviewStatus(@Param("status") ParseReviewStatus status);

    // 오늘 처리된 건수 (PENDING 제외, 삭제된 공고 제외)
    @Query("SELECT COUNT(e) FROM AnnouncementEligibility e WHERE e.reviewedAt >= :reviewedAtStart AND e.reviewStatus <> :excludedStatus AND e.announcement.deleted = false")
    long countByReviewedAtGreaterThanEqualAndReviewStatusNot(@Param("reviewedAtStart") LocalDateTime reviewedAtStart, @Param("excludedStatus") ParseReviewStatus excludedStatus);
}
