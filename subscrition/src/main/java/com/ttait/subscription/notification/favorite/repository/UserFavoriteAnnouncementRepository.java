package com.ttait.subscription.notification.favorite.repository;

import com.ttait.subscription.announcement.domain.ParseReviewStatus;
import com.ttait.subscription.notification.favorite.domain.UserFavoriteAnnouncement;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserFavoriteAnnouncementRepository extends JpaRepository<UserFavoriteAnnouncement, Long> {

    Optional<UserFavoriteAnnouncement> findByUserIdAndAnnouncementId(Long userId, Long announcementId);

    boolean existsByUserIdAndAnnouncementId(Long userId, Long announcementId);

    @Query("SELECT f.announcement.id FROM UserFavoriteAnnouncement f WHERE f.userId = :userId AND f.announcement.id IN :announcementIds")
    Set<Long> findAnnouncementIdsByUserIdAndAnnouncementIdIn(
            @Param("userId") Long userId,
            @Param("announcementIds") Collection<Long> announcementIds);

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
            ORDER BY
              CASE
                WHEN a.applicationStartDate IS NULL OR a.applicationEndDate IS NULL OR a.applicationStartDate > a.applicationEndDate THEN 5
                WHEN :today > a.applicationEndDate THEN 6
                WHEN :today < a.applicationStartDate THEN 4
                WHEN a.applicationEndDate = :today THEN 0
                WHEN a.applicationEndDate = :tomorrow THEN 1
                WHEN a.applicationEndDate <= :sevenDaysLater THEN 2
                ELSE 3
              END ASC,
              CASE
                WHEN :today < a.applicationStartDate THEN a.applicationStartDate
                ELSE a.applicationEndDate
              END ASC,
              CASE
                WHEN a.applicationStartDate IS NULL OR a.applicationEndDate IS NULL OR a.applicationStartDate > a.applicationEndDate THEN f.createdAt
                ELSE NULL
              END DESC,
              a.noticeName ASC,
              a.id ASC
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
    Page<UserFavoriteAnnouncement> findSchedulePageByUserIdWithAnnouncement(
            @Param("userId") Long userId,
            @Param("reviewStatuses") Collection<ParseReviewStatus> reviewStatuses,
            @Param("today") LocalDate today,
            @Param("tomorrow") LocalDate tomorrow,
            @Param("sevenDaysLater") LocalDate sevenDaysLater,
            Pageable pageable);

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
