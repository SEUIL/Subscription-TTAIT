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
}
