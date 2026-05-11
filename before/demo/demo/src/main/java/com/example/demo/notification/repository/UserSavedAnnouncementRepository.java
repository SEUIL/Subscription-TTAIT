package com.example.demo.notification.repository;

import com.example.demo.notification.domain.UserSavedAnnouncement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSavedAnnouncementRepository extends JpaRepository<UserSavedAnnouncement, Long> {
    boolean existsByUserIdAndAnnouncementId(Long userId, Long announcementId);
    Optional<UserSavedAnnouncement> findByUserIdAndAnnouncementId(Long userId, Long announcementId);
    List<UserSavedAnnouncement> findByAnnouncementId(Long announcementId);
    List<UserSavedAnnouncement> findByUserId(Long userId);
}
