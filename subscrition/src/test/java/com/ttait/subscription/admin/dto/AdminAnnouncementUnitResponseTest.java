package com.ttait.subscription.admin.dto;

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

class AdminAnnouncementUnitResponseTest {

    @Test
    void adminResponseMapsGeocodeFieldsAndReviewOnlyMessage() {
        AnnouncementUnit unit = unit();
        LocalDateTime geocodedAt = LocalDateTime.of(2026, 5, 20, 11, 10);
        LocalDateTime addressNormalizedAt = LocalDateTime.of(2026, 5, 20, 10, 30);
        unit.markAddressResolved("경기도 수원시 테스트로 1", "4111110100", "41111", addressNormalizedAt);
        unit.markGeocodeFailed("API 오류", geocodedAt);

        AdminAnnouncementUnitResponse response = AdminAnnouncementUnitResponse.from(unit);

        assertThat(response.unitId()).isEqualTo(10L);
        assertThat(response.rawText()).isEqualTo("원문 행 텍스트");
        assertThat(response.sourceUnitKey()).isEqualTo("unit-key-1");
        assertThat(response.unitSource()).isEqualTo("MERGED");
        assertThat(response.matchSource()).isEqualTo("AI");
        assertThat(response.confidenceLevel()).isEqualTo("HIGH");
        assertThat(response.legalDongCode()).isEqualTo("4111110100");
        assertThat(response.lawdCd()).isEqualTo("41111");
        assertThat(response.addressStatus()).isEqualTo("SUCCESS");
        assertThat(response.addressMessage()).isNull();
        assertThat(response.addressNormalizedAt()).isEqualTo(addressNormalizedAt);
        assertThat(response.latitude()).isNull();
        assertThat(response.longitude()).isNull();
        assertThat(response.geocodeStatus()).isEqualTo("FAILED");
        assertThat(response.geocodedAt()).isEqualTo(geocodedAt);
        assertThat(response.geocodeMessage()).isEqualTo("API 오류");
        assertThat(recordComponentNames(AdminAnnouncementUnitResponse.class))
                .contains("legalDongCode", "lawdCd", "addressStatus", "addressMessage", "addressNormalizedAt", "geocodeMessage")
                .doesNotContain("rawNaverPayload", "naverPayload", "naverApiKey", "clientSecret");
    }

    @Test
    void adminResponseHandlesNullableGeocodeFields() {
        AnnouncementUnit unit = unit();
        ReflectionTestUtils.setField(unit, "geocodeStatus", null);

        AdminAnnouncementUnitResponse response = AdminAnnouncementUnitResponse.from(unit);

        assertThat(response.legalDongCode()).isNull();
        assertThat(response.lawdCd()).isNull();
        assertThat(response.addressStatus()).isEqualTo("NOT_REQUESTED");
        assertThat(response.addressMessage()).isNull();
        assertThat(response.addressNormalizedAt()).isNull();
        assertThat(response.latitude()).isNull();
        assertThat(response.longitude()).isNull();
        assertThat(response.geocodeStatus()).isNull();
        assertThat(response.geocodedAt()).isNull();
        assertThat(response.geocodeMessage()).isNull();
    }

    private String[] recordComponentNames(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
                .map(component -> component.getName())
                .toArray(String[]::new);
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
}
