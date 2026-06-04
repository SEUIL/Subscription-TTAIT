package com.ttait.subscription.market.service;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.market.domain.MarketSourceType;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MarketSourceTypeResolver {

    private MarketSourceTypeResolver() {
    }

    public static MarketSourceType resolve(AnnouncementUnit unit) {
        return resolve(unit, MarketSourceType.APT_RENT);
    }

    public static MarketSourceType resolve(AnnouncementUnit unit, MarketSourceType requestedSourceType) {
        String text = text(unit);
        boolean trade = requestedSourceType != null && requestedSourceType.name().endsWith("TRADE");
        if (text.contains("오피스텔")) {
            return trade ? MarketSourceType.OFFICETEL_TRADE : MarketSourceType.OFFICETEL_RENT;
        }
        if (text.contains("연립") || text.contains("다세대") || text.contains("다가구")) {
            return trade ? MarketSourceType.ROW_HOUSE_TRADE : MarketSourceType.ROW_HOUSE_RENT;
        }
        return trade ? MarketSourceType.APT_TRADE : MarketSourceType.APT_RENT;
    }

    private static String text(AnnouncementUnit unit) {
        if (unit == null) {
            return "";
        }
        Announcement announcement = unit.getAnnouncement();
        return Stream.of(
                        unit.getHouseTypeNormalized(),
                        unit.getHouseTypeRaw(),
                        unit.getSupplyTypeNormalized(),
                        unit.getSupplyTypeRaw(),
                        unit.getComplexName(),
                        announcement != null ? announcement.getHouseTypeNormalized() : null,
                        announcement != null ? announcement.getHouseTypeRaw() : null,
                        announcement != null ? announcement.getSupplyTypeNormalized() : null,
                        announcement != null ? announcement.getSupplyTypeRaw() : null,
                        announcement != null ? announcement.getComplexName() : null
                )
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
    }
}
