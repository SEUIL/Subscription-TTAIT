package com.ttait.subscription.announcement.domain;

import com.ttait.subscription.common.entity.BaseTimeEntity;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "announcement_eligibility")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnouncementEligibility extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK (자동 증가)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false, unique = true)
    private Announcement announcement; // 연관 공고 (1:1)

    @Column(name = "age_min")
    private Integer ageMin; // 신청 가능 최소 나이

    @Column(name = "age_max")
    private Integer ageMax; // 신청 가능 최대 나이

    @Column(name = "age_raw_text", columnDefinition = "TEXT")
    private String ageRawText; // 나이 조건 원문 (AI 추출)

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_target_type", length = 20)
    private MaritalTargetType maritalTargetType; // 혼인 상태 조건 유형

    @Column(name = "marriage_year_limit")
    private Integer marriageYearLimit; // 혼인 기간 제한 (년, 신혼부부 조건)

    @Column(name = "marital_raw_text", columnDefinition = "TEXT")
    private String maritalRawText; // 혼인 조건 원문 (AI 추출)

    @Column(name = "children_min_count")
    private Integer childrenMinCount; // 자녀 최소 수 (다자녀 조건)

    @Column(name = "children_raw_text", columnDefinition = "TEXT")
    private String childrenRawText; // 자녀 조건 원문 (AI 추출)

    @Column(name = "homeless_required")
    private Boolean homelessRequired; // 무주택 조건 여부

    @Column(name = "homeless_raw_text", columnDefinition = "TEXT")
    private String homelessRawText; // 무주택 조건 원문 (AI 추출)

    @Column(name = "low_income_required")
    private Boolean lowIncomeRequired; // 저소득 조건 여부

    @Column(name = "income_asset_criteria_raw", columnDefinition = "TEXT")
    private String incomeAssetCriteriaRaw; // 소득/자산 기준 원문 (AI 추출)

    @Column(name = "elderly_required")
    private Boolean elderlyRequired; // 고령자 조건 여부

    @Column(name = "elderly_age_min")
    private Integer elderlyAgeMin; // 고령자 최소 나이 기준

    @Column(name = "elderly_raw_text", columnDefinition = "TEXT")
    private String elderlyRawText; // 고령자 조건 원문 (AI 추출)

    @Column(name = "eligibility_raw", columnDefinition = "TEXT")
    private String eligibilityRaw; // 전체 자격 조건 원문 (AI 추출)

    @Column(name = "special_supply_raw", columnDefinition = "TEXT")
    private String specialSupplyRaw; // 특별공급 조건 원문 (AI 추출)

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20, columnDefinition = "VARCHAR(20) NOT NULL DEFAULT 'PENDING'")
    private ParseReviewStatus reviewStatus; // 관리자 검수 상태 (PENDING/APPROVED/CORRECTED/REJECTED)

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy; // 검수한 관리자 로그인 ID

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt; // 검수 처리 시각

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote; // 검수 메모

    @Builder
    public AnnouncementEligibility(Announcement announcement, Integer ageMin, Integer ageMax, String ageRawText,
                                   MaritalTargetType maritalTargetType, Integer marriageYearLimit, String maritalRawText,
                                   Integer childrenMinCount, String childrenRawText,
                                   Boolean homelessRequired, String homelessRawText,
                                   Boolean lowIncomeRequired, String incomeAssetCriteriaRaw,
                                   Boolean elderlyRequired, Integer elderlyAgeMin, String elderlyRawText,
                                   String eligibilityRaw, String specialSupplyRaw) {
        this.announcement = announcement;
        this.ageMin = ageMin;
        this.ageMax = ageMax;
        this.ageRawText = ageRawText;
        this.maritalTargetType = maritalTargetType;
        this.marriageYearLimit = marriageYearLimit;
        this.maritalRawText = maritalRawText;
        this.childrenMinCount = childrenMinCount;
        this.childrenRawText = childrenRawText;
        this.homelessRequired = homelessRequired;
        this.homelessRawText = homelessRawText;
        this.lowIncomeRequired = lowIncomeRequired;
        this.incomeAssetCriteriaRaw = incomeAssetCriteriaRaw;
        this.elderlyRequired = elderlyRequired;
        this.elderlyAgeMin = elderlyAgeMin;
        this.elderlyRawText = elderlyRawText;
        this.eligibilityRaw = eligibilityRaw;
        this.specialSupplyRaw = specialSupplyRaw;
        this.reviewStatus = ParseReviewStatus.PENDING;
    }

    public void update(Integer ageMin, Integer ageMax, String ageRawText,
                       MaritalTargetType maritalTargetType, Integer marriageYearLimit, String maritalRawText,
                       Integer childrenMinCount, String childrenRawText,
                       Boolean homelessRequired, String homelessRawText,
                       Boolean lowIncomeRequired, String incomeAssetCriteriaRaw,
                       Boolean elderlyRequired, Integer elderlyAgeMin, String elderlyRawText,
                       String eligibilityRaw, String specialSupplyRaw) {
        this.ageMin = ageMin;
        this.ageMax = ageMax;
        this.ageRawText = ageRawText;
        this.maritalTargetType = maritalTargetType;
        this.marriageYearLimit = marriageYearLimit;
        this.maritalRawText = maritalRawText;
        this.childrenMinCount = childrenMinCount;
        this.childrenRawText = childrenRawText;
        this.homelessRequired = homelessRequired;
        this.homelessRawText = homelessRawText;
        this.lowIncomeRequired = lowIncomeRequired;
        this.incomeAssetCriteriaRaw = incomeAssetCriteriaRaw;
        this.elderlyRequired = elderlyRequired;
        this.elderlyAgeMin = elderlyAgeMin;
        this.elderlyRawText = elderlyRawText;
        this.eligibilityRaw = eligibilityRaw;
        this.specialSupplyRaw = specialSupplyRaw;
    }

    public void approve(String reviewerLoginId) {
        this.reviewStatus = ParseReviewStatus.APPROVED;
        this.reviewedBy = reviewerLoginId;
        this.reviewedAt = LocalDateTime.now();
    }

    public void correct(String reviewerLoginId, String note,
                        Integer ageMin, Integer ageMax, MaritalTargetType maritalTargetType,
                        Integer marriageYearLimit, Integer childrenMinCount,
                        Boolean homelessRequired, Boolean lowIncomeRequired,
                        Boolean elderlyRequired, Integer elderlyAgeMin) {
        if (ageMin != null) this.ageMin = ageMin;
        if (ageMax != null) this.ageMax = ageMax;
        if (maritalTargetType != null) this.maritalTargetType = maritalTargetType;
        if (marriageYearLimit != null) this.marriageYearLimit = marriageYearLimit;
        if (childrenMinCount != null) this.childrenMinCount = childrenMinCount;
        if (homelessRequired != null) this.homelessRequired = homelessRequired;
        if (lowIncomeRequired != null) this.lowIncomeRequired = lowIncomeRequired;
        if (elderlyRequired != null) this.elderlyRequired = elderlyRequired;
        if (elderlyAgeMin != null) this.elderlyAgeMin = elderlyAgeMin;
        this.reviewStatus = ParseReviewStatus.CORRECTED;
        this.reviewedBy = reviewerLoginId;
        this.reviewedAt = LocalDateTime.now();
        this.reviewNote = note;
    }

    public void reject(String reviewerLoginId, String note) {
        this.reviewStatus = ParseReviewStatus.REJECTED;
        this.reviewedBy = reviewerLoginId;
        this.reviewedAt = LocalDateTime.now();
        this.reviewNote = note;
    }

    public void resetToPending() {
        this.reviewStatus = ParseReviewStatus.PENDING;
        this.reviewedBy = null;
        this.reviewedAt = null;
        this.reviewNote = null;
    }
}
