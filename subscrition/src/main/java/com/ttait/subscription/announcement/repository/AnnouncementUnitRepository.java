package com.ttait.subscription.announcement.repository;

import com.ttait.subscription.announcement.domain.AddressResolutionStatus;
import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.announcement.domain.AnnouncementUnitSource;
import com.ttait.subscription.announcement.domain.GeocodeStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementUnitRepository extends JpaRepository<AnnouncementUnit, Long> {

    List<AnnouncementUnit> findByAnnouncementIdAndDeletedFalseOrderByUnitOrderAsc(Long announcementId);

    List<AnnouncementUnit> findByAnnouncementIdAndGeocodeStatusAndDeletedFalseOrderByUnitOrderAsc(
            Long announcementId,
            GeocodeStatus geocodeStatus);

    List<AnnouncementUnit> findByAnnouncementIdAndAddressStatusAndDeletedFalseOrderByUnitOrderAsc(
            Long announcementId,
            AddressResolutionStatus addressStatus);

    List<AnnouncementUnit> findByAnnouncementIdAndAddressStatusInAndDeletedFalseOrderByUnitOrderAsc(
            Long announcementId,
            List<AddressResolutionStatus> addressStatuses);

    List<AnnouncementUnit> findByAnnouncementIdInAndDeletedFalseOrderByAnnouncementIdAscUnitOrderAsc(List<Long> announcementIds);

    @Query("""
            SELECT u.announcement.id AS announcementId, COUNT(u.id) AS unitCount
            FROM AnnouncementUnit u
            WHERE u.announcement.id IN :announcementIds
              AND u.deleted = false
            GROUP BY u.announcement.id
            """)
    List<UnitCountProjection> countUnitsByAnnouncementIds(@Param("announcementIds") Collection<Long> announcementIds);

    Optional<AnnouncementUnit> findByIdAndAnnouncementIdAndDeletedFalse(Long id, Long announcementId);

    Optional<AnnouncementUnit> findByAnnouncementIdAndUnitSourceAndSourceUnitKeyAndDeletedFalse(
            Long announcementId,
            AnnouncementUnitSource unitSource,
            String sourceUnitKey);

    void deleteByAnnouncementId(Long announcementId);

    interface UnitCountProjection {
        Long getAnnouncementId();

        Long getUnitCount();
    }
}
