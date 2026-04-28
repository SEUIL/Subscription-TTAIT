package com.ttait.subscription.announcement.repository;

import com.ttait.subscription.announcement.domain.AnnouncementCategory;
import com.ttait.subscription.user.domain.enums.CategoryCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementCategoryRepository extends JpaRepository<AnnouncementCategory, Long> {

    boolean existsByAnnouncementIdAndCategoryCode(Long announcementId, CategoryCode categoryCode);

    void deleteByAnnouncementId(Long announcementId);
}
