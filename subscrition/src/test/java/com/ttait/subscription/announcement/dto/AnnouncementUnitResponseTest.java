package com.ttait.subscription.announcement.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.announcement.domain.AnnouncementUnitSource;
import com.ttait.subscription.announcement.domain.ConfidenceLevel;
import com.ttait.subscription.announcement.domain.MatchSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AnnouncementUnitResponseTest {

    @Test
    void publicResponseMapsSafeUnitFieldsWithoutSourceKeyOrRawText() {
        AnnouncementUnit unit = unit();

        AnnouncementUnitResponse response = AnnouncementUnitResponse.from(unit);

        assertThat(response.unitId()).isEqualTo(10L);
        assertThat(response.unitOrder()).isEqualTo(1);
        assertThat(response.complexName()).isEqualTo("테스트단지");
        assertThat(response.supplyType()).isEqualTo("국민임대");
        assertThat(response.houseType()).isEqualTo("아파트");
        assertThat(response.exclusiveAreaValue()).isEqualByComparingTo(new BigDecimal("59.84"));
        assertThat(response.depositAmount()).isEqualTo(5000L);
        assertThat(response.monthlyRentAmount()).isEqualTo(25L);
        assertThat(response.latitude()).isNull();
        assertThat(response.longitude()).isNull();
        assertThat(response.geocodeStatus()).isEqualTo("NOT_REQUESTED");
        assertThat(response.geocodedAt()).isNull();
        assertThat(recordComponentNames()).doesNotContain(
                "rawText",
                "sourceUnitKey",
                "salePriceRaw",
                "matchSource",
                "unitSource",
                "confidenceLevel");
    }

    @Test
    void publicResponseMapsGeocodeFieldsWithoutAdminMessageOrRawPayload() {
        AnnouncementUnit unit = unit();
        LocalDateTime geocodedAt = LocalDateTime.of(2026, 5, 20, 11, 0);
        unit.markGeocodeSuccess(new BigDecimal("37.5665000"), new BigDecimal("126.9780000"), geocodedAt);

        AnnouncementUnitResponse response = AnnouncementUnitResponse.from(unit);

        assertThat(response.latitude()).isEqualByComparingTo(new BigDecimal("37.5665000"));
        assertThat(response.longitude()).isEqualByComparingTo(new BigDecimal("126.9780000"));
        assertThat(response.geocodeStatus()).isEqualTo("SUCCESS");
        assertThat(response.geocodedAt()).isEqualTo(geocodedAt);
        assertThat(recordComponentNames()).doesNotContain(
                "geocodeMessage",
                "rawText",
                "sourceUnitKey",
                "rawNaverPayload",
                "naverPayload",
                "naverApiKey");
    }

    @Test
    void publicResponseHandlesNullableGeocodeStatus() {
        AnnouncementUnit unit = unit();
        ReflectionTestUtils.setField(unit, "geocodeStatus", null);

        AnnouncementUnitResponse response = AnnouncementUnitResponse.from(unit);

        assertThat(response.latitude()).isNull();
        assertThat(response.longitude()).isNull();
        assertThat(response.geocodeStatus()).isNull();
        assertThat(response.geocodedAt()).isNull();
    }

    private AnnouncementUnit unit() {
        AnnouncementUnit unit = AnnouncementUnit.builder()
                .unitSource(AnnouncementUnitSource.MERGED)
                .sourceUnitKey("unit-key-1")
                .unitOrder(1)
                .complexName("테스트단지")
                .fullAddress("경기도 수원시 테스트로 1")
                .regionLevel1("경기도")
                .regionLevel2("수원시")
                .supplyTypeRaw("국민임대 원문")
                .supplyTypeNormalized("국민임대")
                .houseTypeRaw("아파트 원문")
                .houseTypeNormalized("아파트")
                .exclusiveAreaText("59.84㎡")
                .exclusiveAreaValue(new BigDecimal("59.84"))
                .depositAmount(5000L)
                .monthlyRentAmount(25L)
                .salePriceMin(18000L)
                .salePriceMax(22000L)
                .salePriceRaw("분양가 원문")
                .supplyHouseholdCount(30)
                .rawText("원문 행 텍스트")
                .matchSource(MatchSource.AI)
                .confidenceLevel(ConfidenceLevel.HIGH)
                .build();
        ReflectionTestUtils.setField(unit, "id", 10L);
        return unit;
    }

    private String[] recordComponentNames() {
        return Arrays.stream(AnnouncementUnitResponse.class.getRecordComponents())
                .map(component -> component.getName())
                .toArray(String[]::new);
    }
}
