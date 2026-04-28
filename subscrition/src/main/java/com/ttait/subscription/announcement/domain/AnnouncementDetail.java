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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "announcement_detail")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnouncementDetail extends SoftDeleteBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK (자동 증가)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false, unique = true)
    private Announcement announcement; // 연관 공고 (1:1)

    @Column(name = "application_datetime_text", length = 255)
    private String applicationDatetimeText; // 청약 신청 일시 (원문 텍스트)

    @Column(name = "document_submit_start_date")
    private LocalDate documentSubmitStartDate; // 서류 제출 시작일

    @Column(name = "document_submit_end_date")
    private LocalDate documentSubmitEndDate; // 서류 제출 마감일

    @Column(name = "contract_start_date")
    private LocalDate contractStartDate; // 계약 시작일

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate; // 계약 마감일

    @Column(name = "complex_name", length = 255)
    private String complexName; // 단지명

    @Column(name = "complex_address", length = 255)
    private String complexAddress; // 단지 주소

    @Column(name = "complex_detail_address", length = 255)
    private String complexDetailAddress; // 단지 상세 주소

    @Column(name = "household_count")
    private Integer householdCount; // 단지 전체 세대 수

    @Column(name = "heating_type", length = 100)
    private String heatingType; // 난방 방식 (예: 지역난방)

    @Column(name = "exclusive_area_text", length = 100)
    private String exclusiveAreaText; // 전용면적 (원문 텍스트)

    @Column(name = "exclusive_area_value", precision = 10, scale = 2)
    private BigDecimal exclusiveAreaValue; // 전용면적 (숫자, ㎡)

    @Column(name = "move_in_expected_ym", length = 20)
    private String moveInExpectedYm; // 입주 예정 연월 (예: 2025-06)

    @Column(name = "guide_text", columnDefinition = "TEXT")
    private String guideText; // 안내 사항 (원문)

    @Column(name = "contact_phone", length = 50)
    private String contactPhone; // 문의 전화번호

    @Column(name = "contact_address", length = 255)
    private String contactAddress; // 문의처 주소

    @Column(name = "contact_guide_text", columnDefinition = "TEXT")
    private String contactGuideText; // 문의처 안내 (원문)

    @Column(name = "supply_household_count_raw", columnDefinition = "TEXT")
    private String supplyHouseholdCountRaw; // AI가 추출한 공급 세대 수 원문

    @Column(name = "supply_household_count_basis", columnDefinition = "TEXT")
    private String supplyHouseholdCountBasis; // 공급 세대 수 추출 근거 (AI 설명)

    @Enumerated(EnumType.STRING)
    @Column(name = "supply_household_count_confidence", length = 10)
    private ConfidenceLevel supplyHouseholdCountConfidence; // 공급 세대 수 추출 신뢰도

    @Column(name = "deposit_monthly_rent_raw", columnDefinition = "TEXT")
    private String depositMonthlyRentRaw; // AI가 추출한 보증금/월세 원문

    @Column(name = "income_asset_criteria_raw", columnDefinition = "TEXT")
    private String incomeAssetCriteriaRaw; // AI가 추출한 소득/자산 기준 원문

    @Column(name = "contact_raw", columnDefinition = "TEXT")
    private String contactRaw; // AI가 추출한 문의처 원문

    @Column(name = "eligibility_raw", columnDefinition = "TEXT")
    private String eligibilityRaw; // AI가 추출한 자격 조건 원문

    @Builder
    public AnnouncementDetail(Announcement announcement, String applicationDatetimeText,
                              LocalDate documentSubmitStartDate, LocalDate documentSubmitEndDate,
                              LocalDate contractStartDate, LocalDate contractEndDate, String complexName,
                              String complexAddress, String complexDetailAddress, Integer householdCount,
                              String heatingType, String exclusiveAreaText, BigDecimal exclusiveAreaValue,
                              String moveInExpectedYm, String guideText, String contactPhone,
                              String contactAddress, String contactGuideText) {
        this.announcement = announcement;
        this.applicationDatetimeText = applicationDatetimeText;
        this.documentSubmitStartDate = documentSubmitStartDate;
        this.documentSubmitEndDate = documentSubmitEndDate;
        this.contractStartDate = contractStartDate;
        this.contractEndDate = contractEndDate;
        this.complexName = complexName;
        this.complexAddress = complexAddress;
        this.complexDetailAddress = complexDetailAddress;
        this.householdCount = householdCount;
        this.heatingType = heatingType;
        this.exclusiveAreaText = exclusiveAreaText;
        this.exclusiveAreaValue = exclusiveAreaValue;
        this.moveInExpectedYm = moveInExpectedYm;
        this.guideText = guideText;
        this.contactPhone = contactPhone;
        this.contactAddress = contactAddress;
        this.contactGuideText = contactGuideText;
    }

    public void updateFromImport(String applicationDatetimeText, LocalDate documentSubmitStartDate,
                                 LocalDate documentSubmitEndDate, LocalDate contractStartDate, LocalDate contractEndDate,
                                 String complexName, String complexAddress, String complexDetailAddress,
                                 Integer householdCount, String heatingType, String exclusiveAreaText,
                                 BigDecimal exclusiveAreaValue, String moveInExpectedYm, String guideText,
                                 String contactPhone, String contactAddress, String contactGuideText) {
        this.applicationDatetimeText = applicationDatetimeText;
        this.documentSubmitStartDate = documentSubmitStartDate;
        this.documentSubmitEndDate = documentSubmitEndDate;
        this.contractStartDate = contractStartDate;
        this.contractEndDate = contractEndDate;
        this.complexName = complexName;
        this.complexAddress = complexAddress;
        this.complexDetailAddress = complexDetailAddress;
        this.householdCount = householdCount;
        this.heatingType = heatingType;
        this.exclusiveAreaText = exclusiveAreaText;
        this.exclusiveAreaValue = exclusiveAreaValue;
        this.moveInExpectedYm = moveInExpectedYm;
        this.guideText = guideText;
        this.contactPhone = contactPhone;
        this.contactAddress = contactAddress;
        this.contactGuideText = contactGuideText;
    }

    public void updateEligibilityRaw(String eligibilityRaw) {
        if (eligibilityRaw != null) this.eligibilityRaw = eligibilityRaw;
    }

    public void updatePdfParseResult(String supplyHouseholdCountRaw, String supplyHouseholdCountBasis,
                                     ConfidenceLevel supplyHouseholdCountConfidence, String depositMonthlyRentRaw,
                                     String incomeAssetCriteriaRaw, String contactRaw, String eligibilityRaw) {
        if (supplyHouseholdCountRaw != null) this.supplyHouseholdCountRaw = supplyHouseholdCountRaw;
        if (supplyHouseholdCountBasis != null) this.supplyHouseholdCountBasis = supplyHouseholdCountBasis;
        if (supplyHouseholdCountConfidence != null) this.supplyHouseholdCountConfidence = supplyHouseholdCountConfidence;
        if (depositMonthlyRentRaw != null) this.depositMonthlyRentRaw = depositMonthlyRentRaw;
        if (incomeAssetCriteriaRaw != null) this.incomeAssetCriteriaRaw = incomeAssetCriteriaRaw;
        if (contactRaw != null) this.contactRaw = contactRaw;
        if (eligibilityRaw != null) this.eligibilityRaw = eligibilityRaw;
    }
}
