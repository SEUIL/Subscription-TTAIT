package com.ttait.subscription.announcement.domain;

import com.ttait.subscription.common.entity.SoftDeleteBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "announcement_unit",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_announcement_unit_source_key",
                columnNames = {"announcement_id", "unit_source", "source_unit_key"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnouncementUnit extends SoftDeleteBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_source", nullable = false, length = 20)
    private AnnouncementUnitSource unitSource;

    @Column(name = "source_unit_key", nullable = false, length = 200)
    private String sourceUnitKey;

    @Column(name = "unit_order", nullable = false)
    private Integer unitOrder;

    @Column(name = "complex_name", length = 255)
    private String complexName;

    @Column(name = "full_address", length = 500)
    private String fullAddress;

    @Column(name = "region_level1", length = 50)
    private String regionLevel1;

    @Column(name = "region_level2", length = 50)
    private String regionLevel2;

    @Column(name = "supply_type_raw", length = 100)
    private String supplyTypeRaw;

    @Column(name = "supply_type_normalized", length = 50)
    private String supplyTypeNormalized;

    @Column(name = "house_type_raw", length = 100)
    private String houseTypeRaw;

    @Column(name = "house_type_normalized", length = 50)
    private String houseTypeNormalized;

    @Column(name = "exclusive_area_text", length = 100)
    private String exclusiveAreaText;

    @Column(name = "exclusive_area_value", precision = 10, scale = 2)
    private BigDecimal exclusiveAreaValue;

    @Column(name = "deposit_amount")
    private Long depositAmount;

    @Column(name = "monthly_rent_amount")
    private Long monthlyRentAmount;

    @Column(name = "sale_price_min")
    private Long salePriceMin;

    @Column(name = "sale_price_max")
    private Long salePriceMax;

    @Column(name = "sale_price_raw", columnDefinition = "TEXT")
    private String salePriceRaw;

    @Column(name = "supply_household_count")
    private Integer supplyHouseholdCount;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_source", length = 20)
    private MatchSource matchSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level", length = 10)
    private ConfidenceLevel confidenceLevel;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "geocode_status", nullable = false, length = 30)
    private GeocodeStatus geocodeStatus = GeocodeStatus.NOT_REQUESTED;

    @Column(name = "geocode_message", length = 500)
    private String geocodeMessage;

    @Column(name = "geocoded_at")
    private LocalDateTime geocodedAt;

    @Column(name = "normalized_address", length = 500)
    private String normalizedAddress;

    @Column(name = "legal_dong_code", length = 10)
    private String legalDongCode;

    @Column(name = "lawd_cd", length = 5)
    private String lawdCd;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_status", nullable = false, length = 30)
    private AddressResolutionStatus addressStatus = AddressResolutionStatus.NOT_REQUESTED;

    @Column(name = "address_message", length = 500)
    private String addressMessage;

    @Column(name = "address_normalized_at")
    private LocalDateTime addressNormalizedAt;

    @Builder
    public AnnouncementUnit(Announcement announcement,
                            AnnouncementUnitSource unitSource,
                            String sourceUnitKey,
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
                            MatchSource matchSource,
                            ConfidenceLevel confidenceLevel) {
        this.announcement = announcement;
        this.unitSource = unitSource;
        this.sourceUnitKey = sourceUnitKey;
        this.unitOrder = unitOrder;
        this.complexName = complexName;
        this.fullAddress = fullAddress;
        this.regionLevel1 = regionLevel1;
        this.regionLevel2 = regionLevel2;
        this.supplyTypeRaw = supplyTypeRaw;
        this.supplyTypeNormalized = supplyTypeNormalized;
        this.houseTypeRaw = houseTypeRaw;
        this.houseTypeNormalized = houseTypeNormalized;
        this.exclusiveAreaText = exclusiveAreaText;
        this.exclusiveAreaValue = exclusiveAreaValue;
        this.depositAmount = depositAmount;
        this.monthlyRentAmount = monthlyRentAmount;
        this.salePriceMin = salePriceMin;
        this.salePriceMax = salePriceMax;
        this.salePriceRaw = salePriceRaw;
        this.supplyHouseholdCount = supplyHouseholdCount;
        this.rawText = rawText;
        this.matchSource = matchSource;
        this.confidenceLevel = confidenceLevel;
    }

    public void markGeocodeSuccess(BigDecimal latitude, BigDecimal longitude, LocalDateTime geocodedAt) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.geocodeStatus = GeocodeStatus.SUCCESS;
        this.geocodeMessage = null;
        this.geocodedAt = geocodedAt;
    }

    public void markGeocodeNoResult(String geocodeMessage, LocalDateTime geocodedAt) {
        clearCoordinates();
        this.geocodeStatus = GeocodeStatus.NO_RESULT;
        this.geocodeMessage = geocodeMessage;
        this.geocodedAt = geocodedAt;
    }

    public void markGeocodeFailed(String geocodeMessage, LocalDateTime geocodedAt) {
        clearCoordinates();
        this.geocodeStatus = GeocodeStatus.FAILED;
        this.geocodeMessage = geocodeMessage;
        this.geocodedAt = geocodedAt;
    }

    public void markGeocodeSkippedNoAddress(String geocodeMessage, LocalDateTime geocodedAt) {
        clearCoordinates();
        this.geocodeStatus = GeocodeStatus.SKIPPED_NO_ADDRESS;
        this.geocodeMessage = geocodeMessage;
        this.geocodedAt = geocodedAt;
    }

    public void markAddressResolved(String normalizedAddress,
                                    String legalDongCode,
                                    String lawdCd,
                                    LocalDateTime addressNormalizedAt) {
        this.normalizedAddress = normalizedAddress;
        this.legalDongCode = legalDongCode;
        this.lawdCd = lawdCd;
        this.addressStatus = AddressResolutionStatus.SUCCESS;
        this.addressMessage = null;
        this.addressNormalizedAt = addressNormalizedAt;
    }

    public void markAddressNoAddress(String addressMessage, LocalDateTime addressNormalizedAt) {
        clearAddressResolution();
        this.normalizedAddress = null;
        this.addressStatus = AddressResolutionStatus.NO_ADDRESS;
        this.addressMessage = addressMessage;
        this.addressNormalizedAt = addressNormalizedAt;
    }

    public void markAddressNoLawdCode(String normalizedAddress,
                                      String addressMessage,
                                      LocalDateTime addressNormalizedAt) {
        clearAddressResolution();
        this.normalizedAddress = normalizedAddress;
        this.addressStatus = AddressResolutionStatus.NO_LAWD_CODE;
        this.addressMessage = addressMessage;
        this.addressNormalizedAt = addressNormalizedAt;
    }

    private void clearCoordinates() {
        this.latitude = null;
        this.longitude = null;
    }

    private void clearAddressResolution() {
        this.legalDongCode = null;
        this.lawdCd = null;
    }
}
