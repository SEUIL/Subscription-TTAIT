package com.ttait.subscription.market.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "lawd_code_mapping",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lawd_mapping_region_dong",
                columnNames = {"region_level2", "legal_dong_name", "legal_dong_code"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LawdCodeMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "region_level1", length = 50)
    private String regionLevel1;

    @Column(name = "region_level2", nullable = false, length = 50)
    private String regionLevel2;

    @Column(name = "legal_dong_name", nullable = false, length = 100)
    private String legalDongName;

    @Column(name = "legal_dong_code", nullable = false, length = 10)
    private String legalDongCode;

    @Column(name = "lawd_cd", nullable = false, length = 5)
    private String lawdCd;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Builder
    public LawdCodeMapping(String regionLevel1,
                           String regionLevel2,
                           String legalDongName,
                           String legalDongCode,
                           Boolean active) {
        this.regionLevel1 = normalize(regionLevel1);
        this.regionLevel2 = normalize(regionLevel2);
        this.legalDongName = normalize(legalDongName);
        this.legalDongCode = legalDongCode;
        this.lawdCd = legalDongCode == null || legalDongCode.length() < 5 ? null : legalDongCode.substring(0, 5);
        this.active = active == null || active;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().replaceAll("\\s+", " ");
    }
}
