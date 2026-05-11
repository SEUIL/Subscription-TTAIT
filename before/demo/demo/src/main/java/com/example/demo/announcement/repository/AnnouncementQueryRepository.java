package com.example.demo.announcement.repository;

import com.example.demo.announcement.repository.query.AnnouncementDetailRow;
import com.example.demo.announcement.repository.query.AnnouncementListRow;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AnnouncementQueryRepository {
    Page<AnnouncementListRow> search(Long userId, String regionLevel1, String regionLevel2,
                                     String supplyType, String provider, String status,
                                     String keyword, String sort, Pageable pageable);

    Optional<AnnouncementDetailRow> findDetail(Long announcementId);
}
