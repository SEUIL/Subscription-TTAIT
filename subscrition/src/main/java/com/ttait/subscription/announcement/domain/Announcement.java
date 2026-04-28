package com.ttait.subscription.announcement.domain;

import com.ttait.subscription.common.entity.SoftDeleteBaseEntity;
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
    private Long id; // PK (자동 증가)

    @Enumerated(EnumType.STRING)
    @Column(name = "source_primary", nullable = false, length = 20)
    private SourceType sourcePrimary; // 공고 출처 (LH, 마이홈 등)

    @Column(name = "source_notice_id", nullable = false, length = 100)
    private String sourceNoticeId; // 출처 시스템의 원본 공고 ID

    @Column(name = "notice_name", nullable = false, length = 255)
    private String noticeName; // 공고명

    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName; // 공급 기관명 (예: LH한국토지주택공사)

    @Column(name = "source_notice_url", nullable = false, length = 500)
    private String sourceNoticeUrl; // 원본 공고 URL

    @Column(name = "source_pc_url", length = 500)
    private String sourcePcUrl; // PC용 공고 URL

    @Column(name = "source_mobile_url", length = 500)
    private String sourceMobileUrl; // 모바일용 공고 URL

    @Column(name = "notice_status_raw", length = 50)
    private String noticeStatusRaw; // 출처에서 받은 원본 공고 상태 문자열

    @Enumerated(EnumType.STRING)
    @Column(name = "notice_status", nullable = false, length = 20)
    private AnnouncementStatus noticeStatus; // 정규화된 공고 상태 (접수예정/접수중/마감)

    @Column(name = "announcement_date")
    private LocalDate announcementDate; // 공고 게시일

    @Column(name = "application_start_date")
    private LocalDate applicationStartDate; // 청약 신청 시작일

    @Column(name = "application_end_date")
    private LocalDate applicationEndDate; // 청약 신청 마감일

    @Column(name = "winner_announcement_date")
    private LocalDate winnerAnnouncementDate; // 당첨자 발표일

    @Column(name = "region_level1", nullable = false, length = 50)
    private String regionLevel1; // 광역시/도 (예: 서울특별시)

    @Column(name = "region_level2", length = 50)
    private String regionLevel2; // 시/군/구 (예: 강남구)

    @Column(name = "full_address", length = 255)
    private String fullAddress; // 전체 주소

    @Column(name = "legal_code", length = 20)
    private String legalCode; // 법정동 코드

    @Column(name = "complex_name", length = 255)
    private String complexName; // 단지명

    @Column(name = "provider_complex_household_count")
    private Integer providerComplexHouseholdCount; // 공급 기관이 제공한 단지 총 세대 수

    @Column(name = "supply_type_raw", length = 100)
    private String supplyTypeRaw; // 출처에서 받은 원본 공급 유형 문자열

    @Column(name = "supply_type_normalized", length = 50)
    private String supplyTypeNormalized; // 정규화된 공급 유형 (공공임대/분양 등)

    @Column(name = "house_type_raw", length = 100)
    private String houseTypeRaw; // 출처에서 받은 원본 주택 유형 문자열

    @Column(name = "house_type_normalized", length = 50)
    private String houseTypeNormalized; // 정규화된 주택 유형 (아파트/빌라 등)

    @Column(name = "deposit_amount")
    private Long depositAmount; // 보증금 (단위: 만원)

    @Column(name = "monthly_rent_amount")
    private Long monthlyRentAmount; // 월세 (단위: 만원)

    @Column(name = "supply_household_count")
    private Integer supplyHouseholdCount; // 공급 세대 수

    @Column(name = "match_key", nullable = false, length = 200)
    private String matchKey; // 중복 공고 병합 기준 키

    @Column(name = "is_merged", nullable = false)
    private boolean merged; // 중복으로 병합된 공고 여부

    @Column(name = "merged_group_key", length = 200)
    private String mergedGroupKey; // 병합 그룹 식별 키

    @Version
    @Column(name = "version", nullable = false)
    private Long version; // 낙관적 락 버전 (동시 수정 충돌 방지)

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt; // 공고 수집 시각

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

    public void updateSupplyHouseholdCount(Integer count) {
        this.supplyHouseholdCount = count;
    }

    public void updateDepositAndRent(Long depositAmountManwon, Long monthlyRentAmountManwon) {
        if (depositAmountManwon != null) this.depositAmount = depositAmountManwon;
        if (monthlyRentAmountManwon != null) this.monthlyRentAmount = monthlyRentAmountManwon;
    }
}
