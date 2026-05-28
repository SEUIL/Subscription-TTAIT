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

    public AddressEnrichmentResult enrichNotRequestedUnits(Long announcementId) {
        return enrichUnits(announcementId, false);
    }

    public AddressEnrichmentResult enrichUnits(Long announcementId, boolean retryNoLawdCode) {
        List<AddressResolutionStatus> statuses = retryNoLawdCode
                ? List.of(AddressResolutionStatus.NOT_REQUESTED, AddressResolutionStatus.NO_LAWD_CODE)
                : List.of(AddressResolutionStatus.NOT_REQUESTED);
        List<AnnouncementUnit> units = announcementUnitRepository
                .findByAnnouncementIdAndAddressStatusInAndDeletedFalseOrderByUnitOrderAsc(
                        announcementId,
                        statuses
                );

        int successCount = 0;
        int noAddressCount = 0;
        int noLawdCodeCount = 0;
        for (AnnouncementUnit unit : units) {
            addressNormalizationService.normalizeUnitAddress(unit);
            announcementUnitRepository.save(unit);
            if (unit.getAddressStatus() == AddressResolutionStatus.SUCCESS) {
                successCount++;
            } else if (unit.getAddressStatus() == AddressResolutionStatus.NO_ADDRESS) {
                noAddressCount++;
            } else if (unit.getAddressStatus() == AddressResolutionStatus.NO_LAWD_CODE) {
                noLawdCodeCount++;
            }
        }
        return new AddressEnrichmentResult(announcementId, units.size(), successCount, noAddressCount, noLawdCodeCount);
    }

    public record AddressEnrichmentResult(
            Long announcementId,
            int processedCount,
            int successCount,
            int noAddressCount,
            int noLawdCodeCount
    ) {
    }
}
