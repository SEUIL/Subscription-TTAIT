package com.example.demo.announcement.repository;

import com.example.demo.announcement.domain.AnnouncementAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementAttachmentRepository extends JpaRepository<AnnouncementAttachment, Long> {
    List<AnnouncementAttachment> findByAnnouncementIdAndDeletedFalse(Long announcementId);
    boolean existsByAnnouncementIdAndAttachmentUrlAndDeletedFalse(Long announcementId, String attachmentUrl);
}
