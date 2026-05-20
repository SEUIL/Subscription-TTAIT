package com.ttait.subscription.admin.dto;

import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminAnnouncementUnitResponse(
        Long unitId,
        Integer unitOrder,
        String complexName,
        String fullAddress,
        String regionLevel1,
        String regionLevel2,
        String supplyTypeRaw,
        String supplyTypeNormalized,
        String houseTypeRaw,
        String houseTypeNormalized,
        String exclusiveAreaText,
        BigDecimal exclusiveAreaValue,
        Long depositAmount,
        Long monthlyRentAmount,
        Long salePriceMin,
        Long salePriceMax,
        String salePriceRaw,
        Integer supplyHouseholdCount,
        String rawText,
        String unitSource,
        String sourceUnitKey,
        String matchSource,
        String confidenceLevel,
        BigDecimal latitude,
        BigDecimal longitude,
        String geocodeStatus,
        LocalDateTime geocodedAt,
        String geocodeMessage
) {
    public static AdminAnnouncementUnitResponse from(AnnouncementUnit unit) {
        return new AdminAnnouncementUnitResponse(
                unit.getId(),
                unit.getUnitOrder(),
                unit.getComplexName(),
                unit.getFullAddress(),
                unit.getRegionLevel1(),
                unit.getRegionLevel2(),
                unit.getSupplyTypeRaw(),
                unit.getSupplyTypeNormalized(),
                unit.getHouseTypeRaw(),
                unit.getHouseTypeNormalized(),
                unit.getExclusiveAreaText(),
                unit.getExclusiveAreaValue(),
                unit.getDepositAmount(),
                unit.getMonthlyRentAmount(),
                unit.getSalePriceMin(),
                unit.getSalePriceMax(),
                unit.getSalePriceRaw(),
                unit.getSupplyHouseholdCount(),
                unit.getRawText(),
                unit.getUnitSource() != null ? unit.getUnitSource().name() : null,
                unit.getSourceUnitKey(),
                unit.getMatchSource() != null ? unit.getMatchSource().name() : null,
                unit.getConfidenceLevel() != null ? unit.getConfidenceLevel().name() : null,
                unit.getLatitude(),
                unit.getLongitude(),
                unit.getGeocodeStatus() != null ? unit.getGeocodeStatus().name() : null,
                unit.getGeocodedAt(),
                unit.getGeocodeMessage()
        );
    }
}
