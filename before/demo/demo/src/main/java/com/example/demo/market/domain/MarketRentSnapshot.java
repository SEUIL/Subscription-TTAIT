package com.example.demo.market.domain;

import com.example.demo.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "market_rent_snapshot")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketRentSnapshot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private MarketSourceType sourceType;

    @Column(name = "lawd_code", nullable = false, length = 10)
    private String lawdCode;

    @Column(name = "deal_ym", nullable = false, length = 6)
    private String dealYm;

    @Column(name = "building_name", length = 255)
    private String buildingName;

    @Column(name = "legal_dong_name", nullable = false, length = 100)
    private String legalDongName;

    @Column(length = 50)
    private String jibun;

    @Column(name = "road_name", length = 255)
    private String roadName;

    @Column(name = "build_year")
    private Integer buildYear;

    @Column(name = "exclusive_area", nullable = false, precision = 10, scale = 2)
    private BigDecimal exclusiveArea;

    @Column
    private Integer floor;

    @Column(name = "deposit_amount", nullable = false)
    private Long depositAmount;

    @Column(name = "monthly_rent_amount", nullable = false)
    private Long monthlyRentAmount;

    @Column(name = "contract_type", length = 50)
    private String contractType;

    @Builder
    public MarketRentSnapshot(MarketSourceType sourceType, String lawdCode, String dealYm, String buildingName,
                              String legalDongName, String jibun, String roadName, Integer buildYear,
                              BigDecimal exclusiveArea, Integer floor, Long depositAmount, Long monthlyRentAmount,
                              String contractType) {
        this.sourceType = sourceType;
        this.lawdCode = lawdCode;
        this.dealYm = dealYm;
        this.buildingName = buildingName;
        this.legalDongName = legalDongName;
        this.jibun = jibun;
        this.roadName = roadName;
        this.buildYear = buildYear;
        this.exclusiveArea = exclusiveArea;
        this.floor = floor;
        this.depositAmount = depositAmount;
        this.monthlyRentAmount = monthlyRentAmount;
        this.contractType = contractType;
    }
}
