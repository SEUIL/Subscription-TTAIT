package com.ttait.subscription.announcement.repository;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.domain.SourceType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    Optional<Announcement> findByIdAndDeletedFalse(Long id);

    Optional<Announcement> findBySourcePrimaryAndSourceNoticeId(SourceType sourcePrimary, String sourceNoticeId);

    List<Announcement> findByMergedGroupKeyAndDeletedFalse(String mergedGroupKey);

    List<Announcement> findByNoticeStatusAndDeletedFalse(AnnouncementStatus noticeStatus);

    List<Announcement> findByApplicationStartDateAndDeletedFalseAndMergedFalse(LocalDate applicationStartDate);

    List<Announcement> findByApplicationEndDateAndDeletedFalseAndMergedFalse(LocalDate applicationEndDate);

    Page<Announcement> findByDeletedFalseAndMergedFalse(Pageable pageable);

    @Query("select distinct a.regionLevel1 from Announcement a where a.deleted = false and a.merged = false and a.regionLevel1 is not null and a.regionLevel1 <> '' order by a.regionLevel1")
    List<String> findDistinctRegionLevel1();

    @Query("select distinct a.regionLevel2 from Announcement a where a.deleted = false and a.merged = false and a.regionLevel2 is not null and a.regionLevel2 <> '' order by a.regionLevel2")
    List<String> findDistinctRegionLevel2();

    List<Announcement> findByDeletedFalseAndMergedFalseAndRegionLevel1IgnoreCase(String regionLevel1);

    @Query("select distinct a.supplyTypeNormalized from Announcement a where a.deleted = false and a.merged = false and a.supplyTypeNormalized is not null and a.supplyTypeNormalized <> '' order by a.supplyTypeNormalized")
    List<String> findDistinctSupplyTypes();

    @Query("select distinct a.houseTypeNormalized from Announcement a where a.deleted = false and a.merged = false and a.houseTypeNormalized is not null and a.houseTypeNormalized <> '' order by a.houseTypeNormalized")
    List<String> findDistinctHouseTypes();

    @Query("select distinct a.providerName from Announcement a where a.deleted = false and a.merged = false and a.providerName is not null and a.providerName <> '' order by a.providerName")
    List<String> findDistinctProviders();

    long countByDeletedFalseAndMergedFalse();
}
