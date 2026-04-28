package com.ttait.subscription.announcement.repository;

import com.ttait.subscription.announcement.domain.AnnouncementParseRaw;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementParseRawRepository extends JpaRepository<AnnouncementParseRaw, Long> {

    void deleteByAnnouncementIdAndRawType(Long announcementId, String rawType);

    Optional<AnnouncementParseRaw> findByAnnouncementIdAndRawType(Long announcementId, String rawType);
}
