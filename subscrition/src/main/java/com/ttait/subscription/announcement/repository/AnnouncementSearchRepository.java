package com.ttait.subscription.announcement.repository;

import com.ttait.subscription.announcement.domain.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AnnouncementSearchRepository {

    Page<Announcement> searchPublicVisible(AnnouncementSearchCondition condition, Pageable pageable);
}
