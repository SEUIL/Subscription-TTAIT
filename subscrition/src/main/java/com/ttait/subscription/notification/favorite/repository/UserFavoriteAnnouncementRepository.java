package com.ttait.subscription.notification.favorite.repository;

import com.ttait.subscription.announcement.domain.ParseReviewStatus;
import com.ttait.subscription.notification.favorite.domain.UserFavoriteAnnouncement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserFavoriteAnnouncementRepository extends JpaRepository<UserFavoriteAnnouncement, Long> {

    Optional<UserFavoriteAnnouncement> findByUserIdAndAnnouncementId(Long userId, Long announcementId);

    boolean existsByUserIdAndAnnouncementId(Long userId, Long announcementId);

    @Query(value = """
            SELECT f FROM UserFavoriteAnnouncement f
            JOIN FETCH f.announcement a
            WHERE f.userId = :userId
              AND a.deleted = false
              AND a.merged = false
              AND EXISTS (
                  SELECT e.id FROM AnnouncementEligibility e
                  WHERE e.announcement = a
                    AND e.reviewStatus IN :reviewStatuses
              )
            """,
           countQuery = """
            SELECT count(f) FROM UserFavoriteAnnouncement f
            JOIN f.announcement a
            WHERE f.userId = :userId
              AND a.deleted = false
              AND a.merged = false
              AND EXISTS (
                  SELECT e.id FROM AnnouncementEligibility e
                  WHERE e.announcement = a
                    AND e.reviewStatus IN :reviewStatuses
              )
            """)
    Page<UserFavoriteAnnouncement> findVisibleByUserIdWithAnnouncement(
            @Param("userId") Long userId,
            @Param("reviewStatuses") Collection<ParseReviewStatus> reviewStatuses,
            Pageable pageable);

    @Query("""
            SELECT f FROM UserFavoriteAnnouncement f
            JOIN FETCH f.announcement a
            WHERE f.userId = :userId
              AND a.deleted = false
              AND a.merged = false
              AND EXISTS (
                  SELECT e.id FROM AnnouncementEligibility e
                  WHERE e.announcement = a
                    AND e.reviewStatus IN :reviewStatuses
              )
            """)
    List<UserFavoriteAnnouncement> findVisibleByUserIdWithAnnouncement(
            @Param("userId") Long userId,
            @Param("reviewStatuses") Collection<ParseReviewStatus> reviewStatuses);

    @Query("""
            SELECT f FROM UserFavoriteAnnouncement f
            JOIN FETCH f.announcement a
            WHERE f.userId = :userId
              AND a.deleted = false
              AND a.merged = false
              AND a.applicationEndDate IS NOT NULL
              AND EXISTS (
                  SELECT e.id FROM AnnouncementEligibility e
                  WHERE e.announcement = a
                    AND e.reviewStatus IN :reviewStatuses
              )
            """)
    List<UserFavoriteAnnouncement> findActiveByUserId(
            @Param("userId") Long userId,
            @Param("reviewStatuses") Collection<ParseReviewStatus> reviewStatuses);

    @Query("""
            SELECT CASE WHEN count(f) > 0 THEN true ELSE false END
            FROM UserFavoriteAnnouncement f
            JOIN f.announcement a
            WHERE f.userId = :userId
              AND a.id = :announcementId
              AND a.deleted = false
              AND a.merged = false
              AND EXISTS (
                  SELECT e.id FROM AnnouncementEligibility e
                  WHERE e.announcement = a
                    AND e.reviewStatus IN :reviewStatuses
              )
            """)
    boolean existsVisibleByUserIdAndAnnouncementId(
            @Param("userId") Long userId,
            @Param("announcementId") Long announcementId,
            @Param("reviewStatuses") Collection<ParseReviewStatus> reviewStatuses);
}
