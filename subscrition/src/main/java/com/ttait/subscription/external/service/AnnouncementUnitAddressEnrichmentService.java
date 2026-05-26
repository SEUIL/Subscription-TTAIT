package com.ttait.subscription.external.service;

import com.ttait.subscription.announcement.domain.AddressResolutionStatus;
import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.announcement.repository.AnnouncementUnitRepository;
import com.ttait.subscription.market.service.AddressNormalizationService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementUnitAddressEnrichmentService {

    private final AnnouncementUnitRepository announcementUnitRepository;
    private final AddressNormalizationService addressNormalizationService;

    public AnnouncementUnitAddressEnrichmentService(AnnouncementUnitRepository announcementUnitRepository,
                                                    AddressNormalizationService addressNormalizationService) {
        this.announcementUnitRepository = announcementUnitRepository;
        this.addressNormalizationService = addressNormalizationService;
    }

    public void enrichNotRequestedUnits(Long announcementId) {
        List<AnnouncementUnit> units = announcementUnitRepository
                .findByAnnouncementIdAndAddressStatusAndDeletedFalseOrderByUnitOrderAsc(
                        announcementId,
                        AddressResolutionStatus.NOT_REQUESTED
                );

        for (AnnouncementUnit unit : units) {
            addressNormalizationService.normalizeUnitAddress(unit);
            announcementUnitRepository.save(unit);
        }
    }
}
