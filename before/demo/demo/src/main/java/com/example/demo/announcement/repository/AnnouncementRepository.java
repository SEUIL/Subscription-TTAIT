package com.example.demo.announcement.repository;

import com.example.demo.announcement.domain.Announcement;
import com.example.demo.announcement.domain.AnnouncementStatus;
import com.example.demo.announcement.domain.SourceType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long>, AnnouncementQueryRepository {
    Optional<Announcement> findByIdAndDeletedFalse(Long id);
    Optional<Announcement> findBySourcePrimaryAndSourceNoticeId(SourceType sourcePrimary, String sourceNoticeId);
    List<Announcement> findByMergedGroupKeyAndDeletedFalse(String mergedGroupKey);
    List<Announcement> findByNoticeStatusAndDeletedFalse(AnnouncementStatus noticeStatus);
    List<Announcement> findByApplicationStartDateAndDeletedFalseAndMergedFalse(java.time.LocalDate applicationStartDate);
    List<Announcement> findByApplicationEndDateAndDeletedFalseAndMergedFalse(java.time.LocalDate applicationEndDate);
    Page<Announcement> findByDeletedFalse(Pageable pageable);

    @Query("select distinct a.regionLevel1 from Announcement a where a.deleted = false and a.merged = false and a.regionLevel1 is not null and a.regionLevel1 <> '' order by a.regionLevel1")
    List<String> findDistinctRegionLevel1();

    @Query("select distinct a.supplyTypeNormalized from Announcement a where a.deleted = false and a.merged = false and a.supplyTypeNormalized is not null and a.supplyTypeNormalized <> '' order by a.supplyTypeNormalized")
    List<String> findDistinctSupplyTypes();

    @Query("select distinct a.providerName from Announcement a where a.deleted = false and a.merged = false and a.providerName is not null and a.providerName <> '' order by a.providerName")
    List<String> findDistinctProviders();
}
