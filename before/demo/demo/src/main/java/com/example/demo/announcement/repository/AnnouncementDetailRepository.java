package com.example.demo.announcement.repository;

import com.example.demo.announcement.domain.AnnouncementDetail;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementDetailRepository extends JpaRepository<AnnouncementDetail, Long> {
    Optional<AnnouncementDetail> findByAnnouncementIdAndDeletedFalse(Long announcementId);
}
