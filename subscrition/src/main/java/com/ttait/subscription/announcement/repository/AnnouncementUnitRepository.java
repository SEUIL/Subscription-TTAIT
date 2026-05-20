package com.ttait.subscription.announcement.repository;

import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.announcement.domain.AnnouncementUnitSource;
import com.ttait.subscription.announcement.domain.GeocodeStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementUnitRepository extends JpaRepository<AnnouncementUnit, Long> {

    List<AnnouncementUnit> findByAnnouncementIdAndDeletedFalseOrderByUnitOrderAsc(Long announcementId);

    List<AnnouncementUnit> findByAnnouncementIdAndGeocodeStatusAndDeletedFalseOrderByUnitOrderAsc(
            Long announcementId,
            GeocodeStatus geocodeStatus);

    List<AnnouncementUnit> findByAnnouncementIdInAndDeletedFalseOrderByAnnouncementIdAscUnitOrderAsc(List<Long> announcementIds);

    Optional<AnnouncementUnit> findByAnnouncementIdAndUnitSourceAndSourceUnitKeyAndDeletedFalse(
            Long announcementId,
            AnnouncementUnitSource unitSource,
            String sourceUnitKey);

    void deleteByAnnouncementId(Long announcementId);
}
