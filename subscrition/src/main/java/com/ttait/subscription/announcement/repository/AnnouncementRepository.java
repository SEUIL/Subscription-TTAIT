package com.ttait.subscription.announcement.repository;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.domain.ParseReviewStatus;
import com.ttait.subscription.announcement.domain.SourceType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    Optional<Announcement> findByIdAndDeletedFalse(Long id);

    Optional<Announcement> findBySourcePrimaryAndSourceNoticeId(SourceType sourcePrimary, String sourceNoticeId);

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.sourcePrimary = :sourcePrimary
              AND a.sourceNoticeId = :sourceNoticeId
            ORDER BY a.deleted ASC, a.collectedAt DESC, a.id DESC
            """)
    List<Announcement> findSourcePairCandidates(@Param("sourcePrimary") SourceType sourcePrimary,
                                                @Param("sourceNoticeId") String sourceNoticeId);

    List<Announcement> findByMergedGroupKeyAndDeletedFalse(String mergedGroupKey);

    List<Announcement> findByNoticeStatusAndDeletedFalse(AnnouncementStatus noticeStatus);

    List<Announcement> findByApplicationStartDateAndDeletedFalseAndMergedFalse(LocalDate applicationStartDate);

    List<Announcement> findByApplicationEndDateAndDeletedFalseAndMergedFalse(LocalDate applicationEndDate);

    Page<Announcement> findByDeletedFalseAndMergedFalse(Pageable pageable);

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.deleted = false
              AND a.merged = false
              AND EXISTS (
                  SELECT e.id FROM AnnouncementEligibility e
                  WHERE e.announcement = a
                    AND e.reviewStatus IN :reviewStatuses
              )
            """)
    Page<Announcement> findPublicVisible(@Param("reviewStatuses") Collection<ParseReviewStatus> reviewStatuses,
                                         Pageable pageable);

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.id = :id
              AND a.deleted = false
              AND a.merged = false
              AND EXISTS (
                  SELECT e.id FROM AnnouncementEligibility e
                  WHERE e.announcement = a
                    AND e.reviewStatus IN :reviewStatuses
              )
            """)
    Optional<Announcement> findPublicVisibleById(@Param("id") Long id,
                                                 @Param("reviewStatuses") Collection<ParseReviewStatus> reviewStatuses);

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.deleted = false
              AND a.merged = false
              AND lower(a.regionLevel1) = lower(:regionLevel1)
              AND EXISTS (
                  SELECT e.id FROM AnnouncementEligibility e
                  WHERE e.announcement = a
                    AND e.reviewStatus IN :reviewStatuses
              )
            """)
    List<Announcement> findPublicVisibleByRegionLevel1IgnoreCase(
            @Param("regionLevel1") String regionLevel1,
            @Param("reviewStatuses") Collection<ParseReviewStatus> reviewStatuses);

    @Query("select distinct a.regionLevel1 from Announcement a where a.deleted = false and a.merged = false and a.regionLevel1 is not null and a.regionLevel1 <> '' order by a.regionLevel1")
    List<String> findDistinctRegionLevel1();

    @Query("""
            SELECT DISTINCT a.regionLevel1 FROM Announcement a
            WHERE a.deleted = false
              AND a.merged = false
              AND a.regionLevel1 IS NOT NULL
              AND a.regionLevel1 <> ''
              AND EXISTS (
                  SELECT e.id FROM AnnouncementEligibility e
                  WHERE e.announcement = a
                    AND e.reviewStatus IN :reviewStatuses
              )
            ORDER BY a.regionLevel1
            """)
    List<String> findDistinctPublicVisibleRegionLevel1(
            @Param("reviewStatuses") Collection<ParseReviewStatus> reviewStatuses);

    @Query("select distinct a.regionLevel2 from Announcement a where a.deleted = false and a.merged = false and a.regionLevel2 is not null and a.regionLevel2 <> '' order by a.regionLevel2")
    List<String> findDistinctRegionLevel2();

    List<Announcement> findByDeletedFalseAndMergedFalseAndRegionLevel1IgnoreCase(String regionLevel1);

    @Query("select distinct a.supplyTypeNormalized from Announcement a where a.deleted = false and a.merged = false and a.supplyTypeNormalized is not null and a.supplyTypeNormalized <> '' order by a.supplyTypeNormalized")
    List<String> findDistinctSupplyTypes();

    @Query("""
            SELECT DISTINCT a.supplyTypeNormalized FROM Announcement a
            WHERE a.deleted = false
              AND a.merged = false
              AND a.supplyTypeNormalized IS NOT NULL
              AND a.supplyTypeNormalized <> ''
              AND EXISTS (
                  SELECT e.id FROM AnnouncementEligibility e
                  WHERE e.announcement = a
                    AND e.reviewStatus IN :reviewStatuses
              )
            ORDER BY a.supplyTypeNormalized
            """)
    List<String> findDistinctPublicVisibleSupplyTypes(
            @Param("reviewStatuses") Collection<ParseReviewStatus> reviewStatuses);

    @Query("select distinct a.houseTypeNormalized from Announcement a where a.deleted = false and a.merged = false and a.houseTypeNormalized is not null and a.houseTypeNormalized <> '' order by a.houseTypeNormalized")
    List<String> findDistinctHouseTypes();

    @Query("select distinct a.providerName from Announcement a where a.deleted = false and a.merged = false and a.providerName is not null and a.providerName <> '' order by a.providerName")
    List<String> findDistinctProviders();

    @Query("""
            SELECT DISTINCT a.providerName FROM Announcement a
            WHERE a.deleted = false
              AND a.merged = false
              AND a.providerName IS NOT NULL
              AND a.providerName <> ''
              AND EXISTS (
                  SELECT e.id FROM AnnouncementEligibility e
                  WHERE e.announcement = a
                    AND e.reviewStatus IN :reviewStatuses
              )
            ORDER BY a.providerName
            """)
    List<String> findDistinctPublicVisibleProviders(
            @Param("reviewStatuses") Collection<ParseReviewStatus> reviewStatuses);

    long countByDeletedFalseAndMergedFalse();
}
