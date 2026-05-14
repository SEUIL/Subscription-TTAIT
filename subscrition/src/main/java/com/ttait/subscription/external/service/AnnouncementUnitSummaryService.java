package com.ttait.subscription.external.service;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.announcement.repository.AnnouncementUnitRepository;
import com.ttait.subscription.external.support.AnnouncementNormalizer;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementUnitSummaryService {

    private final AnnouncementUnitRepository announcementUnitRepository;
    private final AnnouncementNormalizer normalizer;

    public AnnouncementUnitSummaryService(AnnouncementUnitRepository announcementUnitRepository,
                                          AnnouncementNormalizer normalizer) {
        this.announcementUnitRepository = announcementUnitRepository;
        this.normalizer = normalizer;
    }

    public void applySummary(Announcement announcement) {
        List<AnnouncementUnit> units = announcementUnitRepository
                .findByAnnouncementIdAndDeletedFalseOrderByUnitOrderAsc(announcement.getId());
        if (units.isEmpty()) {
            return;
        }

        String fullAddress = firstNonBlank(units.stream().map(AnnouncementUnit::getFullAddress).toList());
        String complexName = firstNonBlank(units.stream().map(AnnouncementUnit::getComplexName).toList());
        String houseTypeRaw = firstNonBlank(units.stream().map(AnnouncementUnit::getHouseTypeRaw).toList());
        String houseTypeNormalized = firstNonBlank(units.stream().map(AnnouncementUnit::getHouseTypeNormalized).toList());
        if (houseTypeNormalized == null && houseTypeRaw != null) {
            houseTypeNormalized = normalizer.normalizeHouseType(houseTypeRaw);
        }

        Long minDeposit = units.stream()
                .map(AnnouncementUnit::getDepositAmount)
                .filter(Objects::nonNull)
                .min(Long::compareTo)
                .orElse(null);
        Long minRent = units.stream()
                .map(AnnouncementUnit::getMonthlyRentAmount)
                .filter(Objects::nonNull)
                .min(Long::compareTo)
                .orElse(null);
        Integer totalSupply = units.stream()
                .map(AnnouncementUnit::getSupplyHouseholdCount)
                .filter(Objects::nonNull)
                .reduce(Integer::sum)
                .orElse(null);

        announcement.updateUnitSummary(
                fullAddress,
                complexName,
                houseTypeRaw,
                houseTypeNormalized,
                minDeposit,
                minRent,
                totalSupply);
    }

    private String firstNonBlank(List<String> values) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }
}
