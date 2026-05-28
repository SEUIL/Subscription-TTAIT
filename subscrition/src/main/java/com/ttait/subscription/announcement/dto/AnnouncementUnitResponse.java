package com.ttait.subscription.announcement.dto;

import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AnnouncementUnitResponse(
        Long unitId,
        Integer unitOrder,
        String complexName,
        String fullAddress,
        String regionLevel1,
        String regionLevel2,
        String supplyType,
        String houseType,
        String exclusiveAreaText,
        BigDecimal exclusiveAreaValue,
        Long depositAmount,
        Long monthlyRentAmount,
        Long salePriceMin,
        Long salePriceMax,
        Integer supplyHouseholdCount,
        BigDecimal latitude,
        BigDecimal longitude,
        String geocodeStatus,
        LocalDateTime geocodedAt
) {
    public static AnnouncementUnitResponse from(AnnouncementUnit unit) {
        return new AnnouncementUnitResponse(
                unit.getId(),
                unit.getUnitOrder(),
                unit.getComplexName(),
                unit.getFullAddress(),
                unit.getRegionLevel1(),
                unit.getRegionLevel2(),
                unit.getSupplyTypeNormalized(),
                unit.getHouseTypeNormalized(),
                unit.getExclusiveAreaText(),
                unit.getExclusiveAreaValue(),
                unit.getDepositAmount(),
                unit.getMonthlyRentAmount(),
                unit.getSalePriceMin(),
                unit.getSalePriceMax(),
                unit.getSupplyHouseholdCount(),
                unit.getLatitude(),
                unit.getLongitude(),
                unit.getGeocodeStatus() != null ? unit.getGeocodeStatus().name() : null,
                unit.getGeocodedAt()
        );
    }
}
