package com.ttait.subscription.market.domain;

import com.ttait.subscription.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
        name = "market_price_snapshot",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_market_price_snapshot_key",
                columnNames = "snapshot_key"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketPriceSnapshot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private MarketSourceType sourceType;

    @Column(name = "lawd_cd", nullable = false, length = 5)
    private String lawdCd;

    @Column(name = "deal_ym_from", nullable = false, length = 6)
    private String dealYmFrom;

    @Column(name = "deal_ym_to", nullable = false, length = 6)
    private String dealYmTo;

    @Column(name = "area_min", precision = 10, scale = 2)
    private BigDecimal areaMin;

    @Column(name = "area_max", precision = 10, scale = 2)
    private BigDecimal areaMax;

    @Column(name = "sample_count", nullable = false)
    private int sampleCount;

    @Column(name = "avg_deposit_amount")
    private Long avgDepositAmount;

    @Column(name = "median_deposit_amount")
    private Long medianDepositAmount;

    @Column(name = "avg_monthly_rent_amount")
    private Long avgMonthlyRentAmount;

    @Column(name = "median_monthly_rent_amount")
    private Long medianMonthlyRentAmount;

    @Column(name = "avg_trade_amount")
    private Long avgTradeAmount;

    @Column(name = "median_trade_amount")
    private Long medianTradeAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MarketSnapshotStatus status;

    @Column(name = "snapshot_key", nullable = false, length = 128)
    private String snapshotKey;

    @Column(name = "aggregated_at", nullable = false)
    private LocalDateTime aggregatedAt;

    @Builder
    public MarketPriceSnapshot(MarketSourceType sourceType,
                               String lawdCd,
                               String dealYmFrom,
                               String dealYmTo,
                               BigDecimal areaMin,
                               BigDecimal areaMax,
                               Integer sampleCount,
                               Long avgDepositAmount,
                               Long medianDepositAmount,
                               Long avgMonthlyRentAmount,
                               Long medianMonthlyRentAmount,
                               Long avgTradeAmount,
                               Long medianTradeAmount,
                               MarketSnapshotStatus status,
                               String snapshotKey,
                               LocalDateTime aggregatedAt) {
        this.sourceType = sourceType;
        this.lawdCd = lawdCd;
        this.dealYmFrom = dealYmFrom;
        this.dealYmTo = dealYmTo;
        this.areaMin = areaMin;
        this.areaMax = areaMax;
        this.sampleCount = sampleCount == null ? 0 : sampleCount;
        this.avgDepositAmount = avgDepositAmount;
        this.medianDepositAmount = medianDepositAmount;
        this.avgMonthlyRentAmount = avgMonthlyRentAmount;
        this.medianMonthlyRentAmount = medianMonthlyRentAmount;
        this.avgTradeAmount = avgTradeAmount;
        this.medianTradeAmount = medianTradeAmount;
        this.status = status == null ? MarketSnapshotStatus.OK : status;
        this.snapshotKey = snapshotKey;
        this.aggregatedAt = aggregatedAt == null ? LocalDateTime.now() : aggregatedAt;
    }
}
