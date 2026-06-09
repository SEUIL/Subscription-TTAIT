package com.ttait.subscription.announcement.service;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementEligibility;
import com.ttait.subscription.announcement.domain.MaritalTargetType;
import com.ttait.subscription.announcement.dto.EligibilityCheckStatus;
import com.ttait.subscription.announcement.dto.EligibilityChecklistItemResponse;
import com.ttait.subscription.announcement.dto.EligibilityChecklistResponse;
import com.ttait.subscription.announcement.dto.EligibilitySummaryStatus;
import com.ttait.subscription.user.domain.UserProfile;
import com.ttait.subscription.user.domain.enums.MaritalStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EligibilityCheckEvaluator {

    static final String DISCLAIMER = "이 결과는 저장된 프로필과 파싱된 공고 정보를 기준으로 한 참고용 체크입니다. 최종 신청 가능 여부는 공고 원문에서 확인해야 합니다.";

    private final Clock clock;

    public EligibilityCheckEvaluator() {
        this(Clock.systemDefaultZone());
    }

    EligibilityCheckEvaluator(Clock clock) {
        this.clock = clock;
    }

    public EligibilityChecklistResponse evaluate(UserProfile profile,
                                                 Announcement announcement,
                                                 AnnouncementEligibility eligibility) {
        List<EligibilityChecklistItemResponse> items = new ArrayList<>();
        items.add(age(profile, eligibility));
        items.add(marital(profile, eligibility));
        items.add(newlywed(profile, eligibility));
        items.add(children(profile, eligibility));
        items.add(homeless(profile, eligibility));
        items.add(lowIncome(profile, eligibility));
        items.add(elderly(profile, eligibility));
        items.add(depositBudget(profile, announcement));
        items.add(monthlyRentBudget(profile, announcement));
        items.add(applicationPeriod(announcement, LocalDate.now(clock)));
        items.add(incomeAssets(eligibility));

        EligibilitySummaryStatus summaryStatus = summaryStatus(eligibility, items);
        return new EligibilityChecklistResponse(
                announcement.getId(),
                summaryStatus,
                summaryMessage(summaryStatus),
                countByStatus(items, EligibilityCheckStatus.MET),
                countByStatus(items, EligibilityCheckStatus.NOT_MET),
                countByStatus(items, EligibilityCheckStatus.NEEDS_VERIFICATION),
                countByStatus(items, EligibilityCheckStatus.NOT_APPLICABLE),
                List.copyOf(items),
                DISCLAIMER
        );
    }

    private int countByStatus(List<EligibilityChecklistItemResponse> items, EligibilityCheckStatus status) {
        return (int) items.stream()
                .filter(item -> item.status() == status)
                .count();
    }

    private EligibilityChecklistItemResponse age(UserProfile profile, AnnouncementEligibility eligibility) {
        if (eligibility == null || (eligibility.getAgeMin() == null && eligibility.getAgeMax() == null)) {
            return item("AGE", "나이", "기본 자격", EligibilityCheckStatus.NOT_APPLICABLE,
                    "INFO", "파싱된 나이 제한이 없습니다.", profile.getAge() != null ? profile.getAge() + "세" : "미입력", "공고 기준 없음", "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        if (profile.getAge() == null) {
            return item("AGE", "나이", "기본 자격", EligibilityCheckStatus.NEEDS_VERIFICATION,
                    "WARNING", "프로필 나이 정보가 부족합니다.", "미입력", ageCondition(eligibility), "프로필 수정", "PROFILE");
        }
        if (eligibility.getAgeMin() != null && profile.getAge() < eligibility.getAgeMin()) {
            return item("AGE", "나이", "기본 자격", EligibilityCheckStatus.NOT_MET,
                    "ERROR", "저장된 나이가 공고의 최소 나이 조건보다 낮습니다.", profile.getAge() + "세", ageCondition(eligibility), "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        if (eligibility.getAgeMax() != null && profile.getAge() > eligibility.getAgeMax()) {
            return item("AGE", "나이", "기본 자격", EligibilityCheckStatus.NOT_MET,
                    "ERROR", "저장된 나이가 공고의 최대 나이 조건보다 높습니다.", profile.getAge() + "세", ageCondition(eligibility), "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        return item("AGE", "나이", "기본 자격", EligibilityCheckStatus.MET,
                "INFO", "저장된 나이가 파싱된 나이 조건 범위에 포함됩니다.", profile.getAge() + "세", ageCondition(eligibility), null, "NONE");
    }

    private EligibilityChecklistItemResponse marital(UserProfile profile, AnnouncementEligibility eligibility) {
        MaritalTargetType targetType = eligibility != null ? eligibility.getMaritalTargetType() : null;
        if (targetType == null || targetType == MaritalTargetType.ANY || targetType == MaritalTargetType.NEWLYWED) {
            return item("MARITAL", "혼인 상태", "가구 조건", EligibilityCheckStatus.NOT_APPLICABLE,
                    "INFO", "별도 혼인 상태 조건이 없거나 신혼부부 항목에서 확인합니다.", maritalValue(profile), maritalCondition(targetType), "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        if (profile.getMaritalStatus() == null) {
            return item("MARITAL", "혼인 상태", "가구 조건", EligibilityCheckStatus.NEEDS_VERIFICATION,
                    "WARNING", "프로필 혼인 상태 정보가 부족합니다.", "미입력", maritalCondition(targetType), "프로필 수정", "PROFILE");
        }
        boolean met = switch (targetType) {
            case SINGLE -> profile.getMaritalStatus() == MaritalStatus.SINGLE;
            case MARRIED -> profile.getMaritalStatus() == MaritalStatus.MARRIED;
            case ENGAGED -> profile.getMaritalStatus() != MaritalStatus.MARRIED;
            case NEWLYWED, ANY -> true;
        };
        return met
                ? item("MARITAL", "혼인 상태", "가구 조건", EligibilityCheckStatus.MET,
                "INFO", "저장된 혼인 상태가 파싱된 조건과 일치합니다.", maritalValue(profile), maritalCondition(targetType), null, "NONE")
                : item("MARITAL", "혼인 상태", "가구 조건", EligibilityCheckStatus.NOT_MET,
                "ERROR", "저장된 혼인 상태가 파싱된 조건과 일치하지 않습니다.", maritalValue(profile), maritalCondition(targetType), "공고 원문 확인", "OFFICIAL_NOTICE");
    }

    private EligibilityChecklistItemResponse newlywed(UserProfile profile, AnnouncementEligibility eligibility) {
        if (eligibility == null || eligibility.getMaritalTargetType() != MaritalTargetType.NEWLYWED) {
            return item("NEWLYWED", "신혼부부", "가구 조건", EligibilityCheckStatus.NOT_APPLICABLE,
                    "INFO", "신혼부부 전용 조건으로 파싱되지 않았습니다.", newlywedValue(profile), "신혼부부 조건 없음", "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        if (profile.getMaritalStatus() != MaritalStatus.MARRIED) {
            return item("NEWLYWED", "신혼부부", "가구 조건", EligibilityCheckStatus.NOT_MET,
                    "ERROR", "프로필 혼인 상태가 기혼이 아닙니다.", newlywedValue(profile), newlywedCondition(eligibility), "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        if (profile.getMarriageYears() == null || eligibility.getMarriageYearLimit() == null) {
            return item("NEWLYWED", "신혼부부", "가구 조건", EligibilityCheckStatus.NEEDS_VERIFICATION,
                    "WARNING", "혼인 기간 또는 공고의 신혼부부 기간 제한 정보가 부족합니다.", newlywedValue(profile), newlywedCondition(eligibility), "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        return profile.getMarriageYears() <= eligibility.getMarriageYearLimit()
                ? item("NEWLYWED", "신혼부부", "가구 조건", EligibilityCheckStatus.MET,
                "INFO", "저장된 혼인 기간이 파싱된 신혼부부 기간 제한 이내입니다.", newlywedValue(profile), newlywedCondition(eligibility), null, "NONE")
                : item("NEWLYWED", "신혼부부", "가구 조건", EligibilityCheckStatus.NOT_MET,
                "ERROR", "저장된 혼인 기간이 파싱된 신혼부부 기간 제한을 초과합니다.", newlywedValue(profile), newlywedCondition(eligibility), "공고 원문 확인", "OFFICIAL_NOTICE");
    }

    private EligibilityChecklistItemResponse children(UserProfile profile, AnnouncementEligibility eligibility) {
        if (eligibility == null || eligibility.getChildrenMinCount() == null) {
            return item("CHILDREN", "자녀 수", "가구 조건", EligibilityCheckStatus.NOT_APPLICABLE,
                    "INFO", "파싱된 최소 자녀 수 조건이 없습니다.", childrenValue(profile), "공고 기준 없음", "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        if (profile.getChildrenCount() == null) {
            return item("CHILDREN", "자녀 수", "가구 조건", EligibilityCheckStatus.NEEDS_VERIFICATION,
                    "WARNING", "프로필 자녀 수 정보가 부족합니다.", "미입력", childrenCondition(eligibility), "프로필 수정", "PROFILE");
        }
        return profile.getChildrenCount() >= eligibility.getChildrenMinCount()
                ? item("CHILDREN", "자녀 수", "가구 조건", EligibilityCheckStatus.MET,
                "INFO", "저장된 자녀 수가 파싱된 최소 자녀 수 이상입니다.", childrenValue(profile), childrenCondition(eligibility), null, "NONE")
                : item("CHILDREN", "자녀 수", "가구 조건", EligibilityCheckStatus.NOT_MET,
                "ERROR", "저장된 자녀 수가 파싱된 최소 자녀 수보다 적습니다.", childrenValue(profile), childrenCondition(eligibility), "공고 원문 확인", "OFFICIAL_NOTICE");
    }

    private EligibilityChecklistItemResponse homeless(UserProfile profile, AnnouncementEligibility eligibility) {
        if (eligibility == null || !Boolean.TRUE.equals(eligibility.getHomelessRequired())) {
            return item("HOMELESS", "무주택", "주택 보유", EligibilityCheckStatus.NOT_APPLICABLE,
                    "INFO", "무주택 필수 조건으로 파싱되지 않았습니다.", profile.isHomeless() ? "무주택" : "무주택 아님", "공고 기준 없음", "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        return profile.isHomeless()
                ? item("HOMELESS", "무주택", "주택 보유", EligibilityCheckStatus.MET,
                "INFO", "프로필에 무주택으로 저장되어 있습니다.", "무주택", "무주택 필수", "공고 원문 확인", "OFFICIAL_NOTICE")
                : item("HOMELESS", "무주택", "주택 보유", EligibilityCheckStatus.NOT_MET,
                "ERROR", "공고는 무주택 조건이 필요하지만 프로필은 무주택으로 저장되어 있지 않습니다.", "무주택 아님", "무주택 필수", "프로필 확인", "PROFILE");
    }

    private EligibilityChecklistItemResponse lowIncome(UserProfile profile, AnnouncementEligibility eligibility) {
        if (eligibility == null || !Boolean.TRUE.equals(eligibility.getLowIncomeRequired())) {
            return item("LOW_INCOME", "저소득/수급", "소득·자산", EligibilityCheckStatus.NOT_APPLICABLE,
                    "INFO", "저소득 필수 조건으로 파싱되지 않았습니다.", lowIncomeValue(profile), "공고 기준 없음", "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        boolean met = profile.isLowIncome() || profile.isRecipient() || profile.isNearPoverty();
        return met
                ? item("LOW_INCOME", "저소득/수급", "소득·자산", EligibilityCheckStatus.MET,
                "INFO", "프로필의 저소득/수급/차상위 정보 중 하나가 충족됩니다.", lowIncomeValue(profile), "저소득/취약계층 필수", "공고 원문 확인", "OFFICIAL_NOTICE")
                : item("LOW_INCOME", "저소득/수급", "소득·자산", EligibilityCheckStatus.NOT_MET,
                "ERROR", "공고는 저소득 조건이 필요하지만 관련 프로필 값이 충족되지 않습니다.", lowIncomeValue(profile), "저소득/취약계층 필수", "프로필 확인", "PROFILE");
    }

    private EligibilityChecklistItemResponse elderly(UserProfile profile, AnnouncementEligibility eligibility) {
        if (eligibility == null || !Boolean.TRUE.equals(eligibility.getElderlyRequired())) {
            return item("ELDERLY", "고령자", "기본 자격", EligibilityCheckStatus.NOT_APPLICABLE,
                    "INFO", "고령자 필수 조건으로 파싱되지 않았습니다.", elderlyValue(profile), "공고 기준 없음", "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        int minAge = eligibility.getElderlyAgeMin() != null ? eligibility.getElderlyAgeMin() : 65;
        boolean met = profile.isElderly() || (profile.getAge() != null && profile.getAge() >= minAge);
        return met
                ? item("ELDERLY", "고령자", "기본 자격", EligibilityCheckStatus.MET,
                "INFO", "프로필 고령자 여부 또는 나이가 파싱된 고령자 기준을 충족합니다.", elderlyValue(profile), minAge + "세 이상", null, "NONE")
                : item("ELDERLY", "고령자", "기본 자격", EligibilityCheckStatus.NOT_MET,
                "ERROR", "파싱된 고령자 기준을 충족하지 않습니다.", elderlyValue(profile), minAge + "세 이상", "공고 원문 확인", "OFFICIAL_NOTICE");
    }

    private EligibilityChecklistItemResponse depositBudget(UserProfile profile, Announcement announcement) {
        if (profile.getMaxDeposit() == null || announcement.getDepositAmount() == null) {
            return item("DEPOSIT_BUDGET", "보증금 예산", "비용", EligibilityCheckStatus.NEEDS_VERIFICATION,
                    "WARNING", "프로필 최대 보증금 또는 공고 보증금 정보가 부족합니다.", moneyValue(profile.getMaxDeposit()), moneyValue(announcement.getDepositAmount()), "프로필/공고 원문 확인", "PROFILE");
        }
        return announcement.getDepositAmount() <= profile.getMaxDeposit()
                ? item("DEPOSIT_BUDGET", "보증금 예산", "비용", EligibilityCheckStatus.MET,
                "INFO", "공고 보증금이 저장된 최대 보증금 이내입니다.", moneyValue(profile.getMaxDeposit()), moneyValue(announcement.getDepositAmount()), null, "NONE")
                : item("DEPOSIT_BUDGET", "보증금 예산", "비용", EligibilityCheckStatus.NOT_MET,
                "ERROR", "공고 보증금이 저장된 최대 보증금을 초과합니다.", moneyValue(profile.getMaxDeposit()), moneyValue(announcement.getDepositAmount()), "공고 원문 확인", "OFFICIAL_NOTICE");
    }

    private EligibilityChecklistItemResponse monthlyRentBudget(UserProfile profile, Announcement announcement) {
        if (profile.getMaxMonthlyRent() == null || announcement.getMonthlyRentAmount() == null) {
            return item("MONTHLY_RENT_BUDGET", "월세 예산", "비용", EligibilityCheckStatus.NEEDS_VERIFICATION,
                    "WARNING", "프로필 최대 월세 또는 공고 월세 정보가 부족합니다.", moneyValue(profile.getMaxMonthlyRent()), moneyValue(announcement.getMonthlyRentAmount()), "프로필/공고 원문 확인", "PROFILE");
        }
        return announcement.getMonthlyRentAmount() <= profile.getMaxMonthlyRent()
                ? item("MONTHLY_RENT_BUDGET", "월세 예산", "비용", EligibilityCheckStatus.MET,
                "INFO", "공고 월세가 저장된 최대 월세 이내입니다.", moneyValue(profile.getMaxMonthlyRent()), moneyValue(announcement.getMonthlyRentAmount()), null, "NONE")
                : item("MONTHLY_RENT_BUDGET", "월세 예산", "비용", EligibilityCheckStatus.NOT_MET,
                "ERROR", "공고 월세가 저장된 최대 월세를 초과합니다.", moneyValue(profile.getMaxMonthlyRent()), moneyValue(announcement.getMonthlyRentAmount()), "공고 원문 확인", "OFFICIAL_NOTICE");
    }

    private EligibilityChecklistItemResponse applicationPeriod(Announcement announcement, LocalDate today) {
        LocalDate startDate = announcement.getApplicationStartDate();
        LocalDate endDate = announcement.getApplicationEndDate();
        if (startDate == null || endDate == null) {
            return item("APPLICATION_PERIOD", "신청 기간", "일정", EligibilityCheckStatus.NEEDS_VERIFICATION,
                    "WARNING", "신청 시작일 또는 마감일 정보가 부족합니다.", today.toString(), periodCondition(startDate, endDate), "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        if (today.isBefore(startDate)) {
            return item("APPLICATION_PERIOD", "신청 기간", "일정", EligibilityCheckStatus.NEEDS_VERIFICATION,
                    "WARNING", "아직 신청 시작 전입니다.", today.toString(), periodCondition(startDate, endDate), "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        if (today.isAfter(endDate)) {
            return item("APPLICATION_PERIOD", "신청 기간", "일정", EligibilityCheckStatus.NOT_MET,
                    "ERROR", "저장된 신청 마감일이 지났습니다.", today.toString(), periodCondition(startDate, endDate), "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        return item("APPLICATION_PERIOD", "신청 기간", "일정", EligibilityCheckStatus.MET,
                "INFO", "오늘 날짜가 저장된 신청 기간 안에 포함됩니다.", today.toString(), periodCondition(startDate, endDate), "공고 원문 확인", "OFFICIAL_NOTICE");
    }

    private EligibilityChecklistItemResponse incomeAssets(AnnouncementEligibility eligibility) {
        if (eligibility != null && StringUtils.hasText(eligibility.getIncomeAssetCriteriaRaw())) {
            return item("INCOME_ASSETS", "소득·자산 상세 기준", "소득·자산", EligibilityCheckStatus.NEEDS_VERIFICATION,
                    "WARNING", "소득·자산 상세 기준은 원문 대조가 필요합니다.", "프로필 소득/자산 정보 참고", "공고 원문 확인 필요", "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        return item("INCOME_ASSETS", "소득·자산 상세 기준", "소득·자산", EligibilityCheckStatus.NOT_APPLICABLE,
                "INFO", "파싱된 소득·자산 상세 기준이 없습니다.", "프로필 소득/자산 정보 참고", "공고 기준 없음", "공고 원문 확인", "OFFICIAL_NOTICE");
    }

    private EligibilitySummaryStatus summaryStatus(AnnouncementEligibility eligibility, List<EligibilityChecklistItemResponse> items) {
        if (eligibility == null) {
            return EligibilitySummaryStatus.INSUFFICIENT_DATA;
        }
        if (items.stream().anyMatch(item -> item.status() == EligibilityCheckStatus.NOT_MET)) {
            return EligibilitySummaryStatus.HAS_BLOCKERS;
        }
        if (items.stream().anyMatch(item -> item.status() == EligibilityCheckStatus.NEEDS_VERIFICATION)) {
            return EligibilitySummaryStatus.REVIEW_REQUIRED;
        }
        if (items.stream().anyMatch(item -> item.status() == EligibilityCheckStatus.MET)) {
            return EligibilitySummaryStatus.LIKELY_READY;
        }
        return EligibilitySummaryStatus.INSUFFICIENT_DATA;
    }

    private String summaryMessage(EligibilitySummaryStatus status) {
        return switch (status) {
            case LIKELY_READY -> "저장된 정보 기준으로 주요 조건을 충족할 가능성이 높습니다.";
            case REVIEW_REQUIRED -> "대체로 확인 가능하지만 공고 원문 또는 프로필 보완 확인이 필요합니다.";
            case HAS_BLOCKERS -> "저장된 정보 기준으로 충족하지 못한 조건이 있습니다.";
            case INSUFFICIENT_DATA -> "파싱된 자격 조건 정보가 부족해 판단할 수 없습니다.";
        };
    }

    private EligibilityChecklistItemResponse item(String key,
                                                   String label,
                                                   String group,
                                                   EligibilityCheckStatus status,
                                                   String severity,
                                                   String reason,
                                                   String userValue,
                                                   String announcementCondition,
                                                   String actionLabel,
                                                   String actionTarget) {
        return new EligibilityChecklistItemResponse(key, group, label, status, severity, reason, userValue,
                announcementCondition, actionLabel, actionTarget);
    }

    private String ageCondition(AnnouncementEligibility eligibility) {
        String min = eligibility.getAgeMin() != null ? eligibility.getAgeMin() + "세" : "제한 없음";
        String max = eligibility.getAgeMax() != null ? eligibility.getAgeMax() + "세" : "제한 없음";
        return min + " ~ " + max;
    }

    private String maritalValue(UserProfile profile) {
        return profile.getMaritalStatus() != null ? profile.getMaritalStatus().name() : "미입력";
    }

    private String maritalCondition(MaritalTargetType targetType) {
        return targetType != null ? targetType.name() : "공고 기준 없음";
    }

    private String newlywedValue(UserProfile profile) {
        return profile.getMarriageYears() != null ? "혼인 " + profile.getMarriageYears() + "년" : maritalValue(profile);
    }

    private String newlywedCondition(AnnouncementEligibility eligibility) {
        return eligibility.getMarriageYearLimit() != null ? "혼인 " + eligibility.getMarriageYearLimit() + "년 이내" : "공고 원문 확인 필요";
    }

    private String childrenValue(UserProfile profile) {
        return profile.getChildrenCount() != null ? profile.getChildrenCount() + "명" : "미입력";
    }

    private String childrenCondition(AnnouncementEligibility eligibility) {
        return eligibility.getChildrenMinCount() != null ? eligibility.getChildrenMinCount() + "명 이상" : "공고 기준 없음";
    }

    private String lowIncomeValue(UserProfile profile) {
        return profile.isLowIncome() || profile.isRecipient() || profile.isNearPoverty() ? "해당" : "해당 없음";
    }

    private String elderlyValue(UserProfile profile) {
        return profile.getAge() != null ? profile.getAge() + "세" : (profile.isElderly() ? "고령자" : "미입력");
    }

    private String moneyValue(Long amount) {
        return amount != null ? amount + "만원" : "미입력";
    }

    private String periodCondition(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return "공고 원문 확인 필요";
        }
        return startDate + " ~ " + endDate;
    }
}
