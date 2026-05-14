package com.ttait.subscription.announcement.repository;

import com.ttait.subscription.announcement.domain.AnnouncementImportFingerprint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementImportFingerprintRepository extends JpaRepository<AnnouncementImportFingerprint, Long> {

    Optional<AnnouncementImportFingerprint> findByAnnouncementId(Long announcementId);

    boolean existsByAnnouncementId(Long announcementId);
}