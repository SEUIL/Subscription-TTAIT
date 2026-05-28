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
        name = "market_transaction_raw",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_market_transaction_raw_hash",
                columnNames = "raw_payload_hash"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketTransactionRaw extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private MarketSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private MarketTransactionType transactionType;

    @Column(name = "lawd_cd", nullable = false, length = 5)
    private String lawdCd;

    @Column(name = "deal_ym", nullable = false, length = 6)
    private String dealYm;

    @Column(name = "legal_dong_name", length = 100)
    private String legalDongName;

    @Column(name = "building_name", length = 255)
    private String buildingName;

    @Column(name = "jibun", length = 100)
    private String jibun;

    @Column(name = "road_name", length = 255)
    private String roadName;

    @Column(name = "build_year")
    private Integer buildYear;

    @Column(name = "exclusive_area", precision = 10, scale = 2)
    private BigDecimal exclusiveArea;

    @Column(name = "floor")
    private Integer floor;

    @Column(name = "deposit_amount")
    private Long depositAmount;

    @Column(name = "monthly_rent_amount")
    private Long monthlyRentAmount;

    @Column(name = "trade_amount")
    private Long tradeAmount;

    @Column(name = "raw_payload_hash", nullable = false, length = 128)
    private String rawPayloadHash;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Builder
    public MarketTransactionRaw(MarketSourceType sourceType,
                                String lawdCd,
                                String dealYm,
                                String legalDongName,
                                String buildingName,
                                String jibun,
                                String roadName,
                                Integer buildYear,
                                BigDecimal exclusiveArea,
                                Integer floor,
                                Long depositAmount,
                                Long monthlyRentAmount,
                                Long tradeAmount,
                                String rawPayloadHash,
                                String rawPayload,
                                LocalDateTime collectedAt) {
        this.sourceType = sourceType;
        this.transactionType = sourceType != null && sourceType.name().endsWith("RENT")
                ? MarketTransactionType.RENT
                : MarketTransactionType.TRADE;
        this.lawdCd = lawdCd;
        this.dealYm = dealYm;
        this.legalDongName = legalDongName;
        this.buildingName = buildingName;
        this.jibun = jibun;
        this.roadName = roadName;
        this.buildYear = buildYear;
        this.exclusiveArea = exclusiveArea;
        this.floor = floor;
        this.depositAmount = depositAmount;
        this.monthlyRentAmount = monthlyRentAmount;
        this.tradeAmount = tradeAmount;
        this.rawPayloadHash = rawPayloadHash;
        this.rawPayload = rawPayload;
        this.collectedAt = collectedAt == null ? LocalDateTime.now() : collectedAt;
    }
}
