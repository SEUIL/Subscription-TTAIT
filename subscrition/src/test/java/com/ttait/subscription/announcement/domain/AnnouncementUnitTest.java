package com.ttait.subscription.announcement.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AnnouncementUnitTest {

    @Test
    void geocodeStatusDefaultsToNotRequested() {
        AnnouncementUnit unit = AnnouncementUnit.builder().build();

        assertThat(unit.getLatitude()).isNull();
        assertThat(unit.getLongitude()).isNull();
        assertThat(unit.getGeocodeStatus()).isEqualTo(GeocodeStatus.NOT_REQUESTED);
        assertThat(unit.getGeocodeMessage()).isNull();
        assertThat(unit.getGeocodedAt()).isNull();
    }

    @Test
    void markGeocodeSuccessStoresCoordinatesAndClearsMessage() {
        AnnouncementUnit unit = AnnouncementUnit.builder().build();
        LocalDateTime geocodedAt = LocalDateTime.of(2026, 5, 20, 10, 0);

        unit.markGeocodeFailed("이전 실패", geocodedAt.minusMinutes(1));
        unit.markGeocodeSuccess(new BigDecimal("37.5665000"), new BigDecimal("126.9780000"), geocodedAt);

        assertThat(unit.getLatitude()).isEqualByComparingTo(new BigDecimal("37.5665000"));
        assertThat(unit.getLongitude()).isEqualByComparingTo(new BigDecimal("126.9780000"));
        assertThat(unit.getGeocodeStatus()).isEqualTo(GeocodeStatus.SUCCESS);
        assertThat(unit.getGeocodeMessage()).isNull();
        assertThat(unit.getGeocodedAt()).isEqualTo(geocodedAt);
    }

    @Test
    void markGeocodeNoResultClearsCoordinatesAndStoresMessage() {
        AnnouncementUnit unit = geocodedUnit();
        LocalDateTime geocodedAt = LocalDateTime.of(2026, 5, 20, 10, 5);

        unit.markGeocodeNoResult("검색 결과 없음", geocodedAt);

        assertThat(unit.getLatitude()).isNull();
        assertThat(unit.getLongitude()).isNull();
        assertThat(unit.getGeocodeStatus()).isEqualTo(GeocodeStatus.NO_RESULT);
        assertThat(unit.getGeocodeMessage()).isEqualTo("검색 결과 없음");
        assertThat(unit.getGeocodedAt()).isEqualTo(geocodedAt);
    }

    @Test
    void markGeocodeFailedClearsCoordinatesAndStoresMessage() {
        AnnouncementUnit unit = geocodedUnit();
        LocalDateTime geocodedAt = LocalDateTime.of(2026, 5, 20, 10, 10);

        unit.markGeocodeFailed("API 오류", geocodedAt);

        assertThat(unit.getLatitude()).isNull();
        assertThat(unit.getLongitude()).isNull();
        assertThat(unit.getGeocodeStatus()).isEqualTo(GeocodeStatus.FAILED);
        assertThat(unit.getGeocodeMessage()).isEqualTo("API 오류");
        assertThat(unit.getGeocodedAt()).isEqualTo(geocodedAt);
    }

    @Test
    void markGeocodeSkippedNoAddressClearsCoordinatesAndStoresMessage() {
        AnnouncementUnit unit = geocodedUnit();
        LocalDateTime geocodedAt = LocalDateTime.of(2026, 5, 20, 10, 15);

        unit.markGeocodeSkippedNoAddress("주소 없음", geocodedAt);

        assertThat(unit.getLatitude()).isNull();
        assertThat(unit.getLongitude()).isNull();
        assertThat(unit.getGeocodeStatus()).isEqualTo(GeocodeStatus.SKIPPED_NO_ADDRESS);
        assertThat(unit.getGeocodeMessage()).isEqualTo("주소 없음");
        assertThat(unit.getGeocodedAt()).isEqualTo(geocodedAt);
    }

    private AnnouncementUnit geocodedUnit() {
        AnnouncementUnit unit = AnnouncementUnit.builder().build();
        unit.markGeocodeSuccess(
                new BigDecimal("37.5665000"),
                new BigDecimal("126.9780000"),
                LocalDateTime.of(2026, 5, 20, 10, 0)
        );
        return unit;
    }
}
