package com.example.demo.announcement.repository;

import com.example.demo.announcement.domain.AnnouncementCategoryTag;
import com.example.demo.announcement.repository.query.AnnouncementTagRow;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AnnouncementCategoryTagRepository extends JpaRepository<AnnouncementCategoryTag, Long> {
    List<AnnouncementCategoryTag> findByAnnouncementId(Long announcementId);

    @Query("""
            select new com.example.demo.announcement.repository.query.AnnouncementTagRow(
                t.announcement.id,
                t.categoryCode
            )
            from AnnouncementCategoryTag t
            where t.announcement.id in :announcementIds
            """)
    List<AnnouncementTagRow> findTagRowsByAnnouncementIds(List<Long> announcementIds);
}
