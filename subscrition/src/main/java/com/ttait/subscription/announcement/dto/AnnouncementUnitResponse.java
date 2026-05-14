package com.ttait.subscription.announcement.dto;

import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import java.math.BigDecimal;

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
        String salePriceRaw,
        Integer supplyHouseholdCount,
        String unitSource,
        String confidenceLevel
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
                unit.getSalePriceRaw(),
                unit.getSupplyHouseholdCount(),
                unit.getUnitSource() != null ? unit.getUnitSource().name() : null,
                unit.getConfidenceLevel() != null ? unit.getConfidenceLevel().name() : null
        );
    }
}
