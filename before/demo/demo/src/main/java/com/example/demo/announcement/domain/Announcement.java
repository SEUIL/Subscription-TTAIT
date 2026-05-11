package com.example.demo.announcement.domain;

import com.example.demo.common.entity.SoftDeleteBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "announcement")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Announcement extends SoftDeleteBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_primary", nullable = false, length = 20)
    private SourceType sourcePrimary;

    @Column(name = "source_notice_id", nullable = false, length = 100)
    private String sourceNoticeId;

    @Column(name = "notice_name", nullable = false, length = 255)
    private String noticeName;

    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName;

    @Column(name = "source_notice_url", nullable = false, length = 500)
    private String sourceNoticeUrl;

    @Column(name = "source_pc_url", length = 500)
    private String sourcePcUrl;

    @Column(name = "source_mobile_url", length = 500)
    private String sourceMobileUrl;

    @Column(name = "notice_status_raw", length = 50)
    private String noticeStatusRaw;

    @Enumerated(EnumType.STRING)
    @Column(name = "notice_status", nullable = false, length = 20)
    private AnnouncementStatus noticeStatus;

    @Column(name = "announcement_date")
    private LocalDate announcementDate;

    @Column(name = "application_start_date")
    private LocalDate applicationStartDate;

    @Column(name = "application_end_date")
    private LocalDate applicationEndDate;

    @Column(name = "winner_announcement_date")
    private LocalDate winnerAnnouncementDate;

    @Column(name = "region_level1", nullable = false, length = 50)
    private String regionLevel1;

    @Column(name = "region_level2", length = 50)
    private String regionLevel2;

    @Column(name = "full_address", length = 255)
    private String fullAddress;

    @Column(name = "legal_code", length = 20)
    private String legalCode;

    @Column(name = "complex_name", length = 255)
    private String complexName;

    @Column(name = "provider_complex_household_count")
    private Integer providerComplexHouseholdCount;

    @Column(name = "supply_type_raw", length = 100)
    private String supplyTypeRaw;

    @Column(name = "supply_type_normalized", length = 50)
    private String supplyTypeNormalized;

    @Column(name = "house_type_raw", length = 100)
    private String houseTypeRaw;

    @Column(name = "house_type_normalized", length = 50)
    private String houseTypeNormalized;

    @Column(name = "deposit_amount")
    private Long depositAmount;

    @Column(name = "monthly_rent_amount")
    private Long monthlyRentAmount;

    @Column(name = "supply_household_count")
    private Integer supplyHouseholdCount;

    @Column(name = "match_key", nullable = false, length = 200)
    private String matchKey;

    @Column(name = "is_merged", nullable = false)
    private boolean merged;

    @Column(name = "merged_group_key", length = 200)
    private String mergedGroupKey;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Builder
    public Announcement(SourceType sourcePrimary, String sourceNoticeId, String noticeName, String providerName,
                        String sourceNoticeUrl, String sourcePcUrl, String sourceMobileUrl, String noticeStatusRaw,
                        AnnouncementStatus noticeStatus, LocalDate announcementDate, LocalDate applicationStartDate,
                        LocalDate applicationEndDate, LocalDate winnerAnnouncementDate, String regionLevel1,
                        String regionLevel2, String fullAddress, String legalCode, String complexName,
                        Integer providerComplexHouseholdCount, String supplyTypeRaw, String supplyTypeNormalized,
                        String houseTypeRaw, String houseTypeNormalized, Long depositAmount, Long monthlyRentAmount,
                        Integer supplyHouseholdCount, String matchKey, boolean merged, String mergedGroupKey,
                        LocalDateTime collectedAt) {
        this.sourcePrimary = sourcePrimary;
        this.sourceNoticeId = sourceNoticeId;
        this.noticeName = noticeName;
        this.providerName = providerName;
        this.sourceNoticeUrl = sourceNoticeUrl;
        this.sourcePcUrl = sourcePcUrl;
        this.sourceMobileUrl = sourceMobileUrl;
        this.noticeStatusRaw = noticeStatusRaw;
        this.noticeStatus = noticeStatus;
        this.announcementDate = announcementDate;
        this.applicationStartDate = applicationStartDate;
        this.applicationEndDate = applicationEndDate;
        this.winnerAnnouncementDate = winnerAnnouncementDate;
        this.regionLevel1 = regionLevel1;
        this.regionLevel2 = regionLevel2;
        this.fullAddress = fullAddress;
        this.legalCode = legalCode;
        this.complexName = complexName;
        this.providerComplexHouseholdCount = providerComplexHouseholdCount;
        this.supplyTypeRaw = supplyTypeRaw;
        this.supplyTypeNormalized = supplyTypeNormalized;
        this.houseTypeRaw = houseTypeRaw;
        this.houseTypeNormalized = houseTypeNormalized;
        this.depositAmount = depositAmount;
        this.monthlyRentAmount = monthlyRentAmount;
        this.supplyHouseholdCount = supplyHouseholdCount;
        this.matchKey = matchKey;
        this.merged = merged;
        this.mergedGroupKey = mergedGroupKey;
        this.collectedAt = collectedAt;
    }

    public void updateFromImport(String noticeName, String providerName, String sourceNoticeUrl, String sourcePcUrl,
                                 String sourceMobileUrl, String noticeStatusRaw, AnnouncementStatus noticeStatus,
                                 LocalDate announcementDate, LocalDate applicationStartDate, LocalDate applicationEndDate,
                                 LocalDate winnerAnnouncementDate, String regionLevel1, String regionLevel2,
                                 String fullAddress, String legalCode, String complexName,
                                 Integer providerComplexHouseholdCount, String supplyTypeRaw,
                                 String supplyTypeNormalized, String houseTypeRaw, String houseTypeNormalized,
                                 Long depositAmount, Long monthlyRentAmount, Integer supplyHouseholdCount,
                                 String matchKey, boolean merged, String mergedGroupKey, LocalDateTime collectedAt) {
        this.noticeName = noticeName;
        this.providerName = providerName;
        this.sourceNoticeUrl = sourceNoticeUrl;
        this.sourcePcUrl = sourcePcUrl;
        this.sourceMobileUrl = sourceMobileUrl;
        this.noticeStatusRaw = noticeStatusRaw;
        this.noticeStatus = noticeStatus;
        this.announcementDate = announcementDate;
        this.applicationStartDate = applicationStartDate;
        this.applicationEndDate = applicationEndDate;
        this.winnerAnnouncementDate = winnerAnnouncementDate;
        this.regionLevel1 = regionLevel1;
        this.regionLevel2 = regionLevel2;
        this.fullAddress = fullAddress;
        this.legalCode = legalCode;
        this.complexName = complexName;
        this.providerComplexHouseholdCount = providerComplexHouseholdCount;
        this.supplyTypeRaw = supplyTypeRaw;
        this.supplyTypeNormalized = supplyTypeNormalized;
        this.houseTypeRaw = houseTypeRaw;
        this.houseTypeNormalized = houseTypeNormalized;
        this.depositAmount = depositAmount;
        this.monthlyRentAmount = monthlyRentAmount;
        this.supplyHouseholdCount = supplyHouseholdCount;
        this.matchKey = matchKey;
        this.merged = merged;
        this.mergedGroupKey = mergedGroupKey;
        this.collectedAt = collectedAt;
    }

    public void markMerged() {
        this.merged = true;
    }

    public void markActive() {
        this.merged = false;
    }
}
