package com.ttait.subscription.notification.favorite.repository;

import com.ttait.subscription.notification.favorite.domain.UserFavoriteAnnouncement;
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

    @Query(value = "SELECT f FROM UserFavoriteAnnouncement f JOIN FETCH f.announcement WHERE f.userId = :userId",
           countQuery = "SELECT count(f) FROM UserFavoriteAnnouncement f WHERE f.userId = :userId")
    Page<UserFavoriteAnnouncement> findByUserIdWithAnnouncement(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT f FROM UserFavoriteAnnouncement f JOIN FETCH f.announcement a WHERE f.userId = :userId AND a.deleted = false AND a.applicationEndDate IS NOT NULL")
    List<UserFavoriteAnnouncement> findActiveByUserId(@Param("userId") Long userId);
}
