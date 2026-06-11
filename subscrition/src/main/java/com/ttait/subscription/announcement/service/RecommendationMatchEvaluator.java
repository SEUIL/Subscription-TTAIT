package com.ttait.subscription.announcement.service;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementEligibility;
import com.ttait.subscription.announcement.domain.MaritalTargetType;
import com.ttait.subscription.announcement.dto.RecommendationFactorResponse;
import com.ttait.subscription.announcement.dto.RecommendationFactorStatus;
import com.ttait.subscription.announcement.dto.RecommendationItemResponse;
import com.ttait.subscription.external.support.AnnouncementNormalizer;
import com.ttait.subscription.user.domain.UserProfile;
import com.ttait.subscription.user.domain.enums.CategoryCode;
import com.ttait.subscription.user.domain.enums.MaritalStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RecommendationMatchEvaluator {

    private final AnnouncementNormalizer announcementNormalizer;
    private final Clock clock;

    @Autowired
    public RecommendationMatchEvaluator(AnnouncementNormalizer announcementNormalizer) {
        this(announcementNormalizer, Clock.systemDefaultZone());
    }

    RecommendationMatchEvaluator(AnnouncementNormalizer announcementNormalizer, Clock clock) {
        this.announcementNormalizer = announcementNormalizer;
        this.clock = clock;
    }

    RecommendationEvaluationResult evaluate(Announcement announcement, UserProfile profile,
                                            Set<CategoryCode> userCategories,
                                            Set<CategoryCode> announcementCategories,
                                            AnnouncementEligibility eligibility) {
        String regionLevel2 = resolveRegionLevel2(announcement);
        String houseType = resolveHouseType(announcement);
        List<RecommendationFactorResponse> factors = buildFactors(
                announcement,
                profile,
                userCategories,
                announcementCategories,
                eligibility,
                regionLevel2,
                houseType);

        if (!passesEligibility(profile, eligibility) || !matchesBudget(profile, announcement)) {
            return RecommendationEvaluationResult.notRecommended(factors);
        }

        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (!userCategories.isEmpty() && !announcementCategories.isEmpty()) {
            Set<CategoryCode> intersection = EnumSet.copyOf(announcementCategories);
            intersection.retainAll(userCategories);
            if (!intersection.isEmpty()) {
                score += 35;
                reasons.add("선택한 신분 유형과 일치");
            }
        }

        if (equalsIgnoreCase(profile.getPreferredRegionLevel1(), announcement.getRegionLevel1())) {
            score += 20;
            reasons.add("희망 지역과 일치");
        }
        if (equalsIgnoreCase(profile.getPreferredRegionLevel2(), regionLevel2)) {
            score += 12;
            reasons.add("세부 희망 지역과 일치");
        }
        if (equalsIgnoreCase(profile.getPreferredHouseType(), houseType)) {
            score += 10;
            reasons.add("희망 주택 유형과 일치");
        }
        if (equalsIgnoreCase(profile.getPreferredSupplyType(), announcement.getSupplyTypeNormalized())) {
            score += 8;
            reasons.add("희망 공급 유형과 일치");
        }
        if (profile.getMaxDeposit() != null && announcement.getDepositAmount() != null
                && announcement.getDepositAmount() <= profile.getMaxDeposit()) {
            score += 8;
            reasons.add("보증금 예산 범위 충족");
        }
        if (profile.getMaxMonthlyRent() != null && announcement.getMonthlyRentAmount() != null
                && announcement.getMonthlyRentAmount() <= profile.getMaxMonthlyRent()) {
            score += 8;
            reasons.add("월세 예산 범위 충족");
        }
        if (eligibility != null && Boolean.TRUE.equals(eligibility.getHomelessRequired()) && profile.isHomeless()) {
            score += 8;
            reasons.add("무주택 조건 충족");
        }
        if (eligibility != null && Boolean.TRUE.equals(eligibility.getLowIncomeRequired())
                && (profile.isLowIncome() || profile.isRecipient() || profile.isNearPoverty())) {
            score += 10;
            reasons.add("저소득/취약계층 조건 충족");
        }
        if (eligibility != null && eligibility.getChildrenMinCount() != null
                && profile.getChildrenCount() >= eligibility.getChildrenMinCount()) {
            score += 8;
            reasons.add("자녀 수 조건 충족");
        }
        if (eligibility != null && Boolean.TRUE.equals(eligibility.getElderlyRequired())
                && profile.getAge() >= defaultElderlyAge(eligibility)) {
            score += 8;
            reasons.add("고령자 조건 충족");
        }

        if (score == 0 && reasons.isEmpty()) {
            return RecommendationEvaluationResult.notRecommended(factors);
        }

        return new RecommendationEvaluationResult(true, new RecommendationItemResponse(
                announcement.getId(),
                announcement.getNoticeName(),
                announcement.getProviderName(),
                announcement.getSupplyTypeNormalized(),
                houseType,
                announcement.getRegionLevel1(),
                regionLevel2,
                announcement.getFullAddress(),
                announcement.getComplexName(),
                announcement.getDepositAmount(),
                announcement.getMonthlyRentAmount(),
                announcement.getApplicationStartDate(),
                announcement.getApplicationEndDate(),
                announcement.getNoticeStatus().name(),
                announcement.getSourceNoticeUrl(),
                score,
                List.copyOf(reasons)
        ), List.copyOf(factors));
    }

    Set<CategoryCode> deriveUserCategories(UserProfile profile, List<CategoryCode> selectedCategories) {
        Set<CategoryCode> categories = selectedCategories.isEmpty()
                ? EnumSet.noneOf(CategoryCode.class)
                : EnumSet.copyOf(selectedCategories);

        if (profile.getAge() != null && profile.getAge() >= 19 && profile.getAge() <= 39
                && profile.getMaritalStatus() == MaritalStatus.SINGLE) {
            categories.add(CategoryCode.YOUTH);
        }
        if (profile.getMaritalStatus() == MaritalStatus.MARRIED
                && profile.getMarriageYears() != null
                && profile.getMarriageYears() <= 7) {
            categories.add(CategoryCode.NEWLYWED);
        }
        if (profile.isHomeless()) {
            categories.add(CategoryCode.HOMELESS);
        }
        if (profile.isElderly() || profile.getAge() >= 65) {
            categories.add(CategoryCode.ELDERLY);
        }
        if (profile.isLowIncome() || profile.isRecipient() || profile.isNearPoverty()) {
            categories.add(CategoryCode.LOW_INCOME);
        }
        if (profile.getChildrenCount() != null && profile.getChildrenCount() >= 3) {
            categories.add(CategoryCode.MULTI_CHILD);
        }
        return categories;
    }

    private List<RecommendationFactorResponse> buildFactors(Announcement announcement,
                                                            UserProfile profile,
                                                            Set<CategoryCode> userCategories,
                                                            Set<CategoryCode> announcementCategories,
                                                            AnnouncementEligibility eligibility,
                                                            String regionLevel2,
                                                            String houseType) {
        List<RecommendationFactorResponse> factors = new ArrayList<>();
        factors.add(categoryFactor(userCategories, announcementCategories));
        factors.add(regionFactor(profile.getPreferredRegionLevel1(), announcement.getRegionLevel1(),
                "REGION", "지역", 20));
        factors.add(regionFactor(profile.getPreferredRegionLevel2(), regionLevel2, "DETAIL_REGION", "세부 지역", 12));
        factors.add(preferenceFactor("HOUSE_TYPE", "선호 조건", "주택 유형", profile.getPreferredHouseType(), houseType, 10));
        factors.add(preferenceFactor("SUPPLY_TYPE", "선호 조건", "공급 유형", profile.getPreferredSupplyType(),
                announcement.getSupplyTypeNormalized(), 8));
        factors.add(budgetFactor("DEPOSIT_BUDGET", "보증금", profile.getMaxDeposit(), announcement.getDepositAmount(), 8));
        factors.add(budgetFactor("MONTHLY_RENT_BUDGET", "월세", profile.getMaxMonthlyRent(),
                announcement.getMonthlyRentAmount(), 8));
        factors.add(applicationPeriodFactor(announcement, LocalDate.now(clock)));
        factors.add(incomeAssetFactor(eligibility));
        factors.add(detailedEligibilityFactor(eligibility));
        return factors;
    }

    private RecommendationFactorResponse categoryFactor(Set<CategoryCode> userCategories,
                                                        Set<CategoryCode> announcementCategories) {
        String userValue = categoriesValue(userCategories);
        String announcementValue = categoriesValue(announcementCategories);
        if (userCategories.isEmpty() || announcementCategories.isEmpty()) {
            return factor("CATEGORY", "선호 조건", "신분 유형", RecommendationFactorStatus.UNKNOWN, "정보 부족",
                    "프로필 또는 공고의 신분 유형 정보가 부족해 자동 설명이 제한됩니다.", userValue, announcementValue,
                    "프로필/공고 원문 확인", "PROFILE");
        }

        Set<CategoryCode> intersection = EnumSet.copyOf(announcementCategories);
        intersection.retainAll(userCategories);
        if (!intersection.isEmpty()) {
            return factor("CATEGORY", "선호 조건", "신분 유형", RecommendationFactorStatus.STRONG_MATCH,
                    "추천 점수에 긍정적", "선택한 신분 유형과 공고 유형이 일치합니다.", userValue, announcementValue,
                    null, "NONE");
        }
        return factor("CATEGORY", "선호 조건", "신분 유형", RecommendationFactorStatus.NOT_MATCHED,
                "추천 점수 반영 없음", "선택한 신분 유형과 공고 유형이 명확히 일치하지 않습니다.", userValue, announcementValue,
                "공고 원문 확인", "OFFICIAL_NOTICE");
    }

    private RecommendationFactorResponse regionFactor(String userValue, String announcementValue, String key,
                                                      String label, int score) {
        return preferenceFactor(key, "선호 조건", label, userValue, announcementValue, score);
    }

    private RecommendationFactorResponse preferenceFactor(String key, String group, String label, String userValue,
                                                          String announcementValue, int score) {
        if (!StringUtils.hasText(userValue) || !StringUtils.hasText(announcementValue)) {
            return factor(key, group, label, RecommendationFactorStatus.UNKNOWN, "정보 부족",
                    label + " 정보가 부족해 자동 설명이 제한됩니다.", displayValue(userValue), displayValue(announcementValue),
                    "프로필/공고 원문 확인", "PROFILE");
        }
        if (equalsIgnoreCase(userValue, announcementValue)) {
            return factor(key, group, label, RecommendationFactorStatus.STRONG_MATCH, "추천 점수에 긍정적",
                    "선호 " + label + "과 공고 " + label + "이 일치합니다.", userValue, announcementValue, null, "NONE");
        }
        return factor(key, group, label, RecommendationFactorStatus.NOT_MATCHED, "추천 점수 반영 없음",
                "선호 " + label + "과 공고 " + label + "이 일치하지 않습니다.", userValue, announcementValue,
                "공고 원문 확인", "OFFICIAL_NOTICE");
    }

    private RecommendationFactorResponse budgetFactor(String key, String label, Long userMaxAmount,
                                                      Long announcementAmount, int score) {
        if (userMaxAmount == null || announcementAmount == null) {
            return factor(key, "비용", label, RecommendationFactorStatus.UNKNOWN, "정보 부족",
                    "프로필 예산 또는 공고 비용 정보가 부족합니다.", moneyValue(userMaxAmount), moneyValue(announcementAmount),
                    "프로필/공고 원문 확인", "PROFILE");
        }
        if (announcementAmount <= userMaxAmount) {
            return factor(key, "비용", label, RecommendationFactorStatus.STRONG_MATCH, "추천 점수에 긍정적",
                    "공고 " + label + "이(가) 저장된 예산 범위 안에 있습니다.", moneyValue(userMaxAmount),
                    moneyValue(announcementAmount), null, "NONE");
        }
        return factor(key, "비용", label, RecommendationFactorStatus.NOT_MATCHED, "추천 제외 조건",
                "공고 " + label + "이(가) 저장된 예산을 초과합니다.", moneyValue(userMaxAmount), moneyValue(announcementAmount),
                "공고 원문 확인", "OFFICIAL_NOTICE");
    }

    private RecommendationFactorResponse applicationPeriodFactor(Announcement announcement, LocalDate today) {
        LocalDate startDate = announcement.getApplicationStartDate();
        LocalDate endDate = announcement.getApplicationEndDate();
        String period = periodValue(startDate, endDate);
        if (startDate == null || endDate == null) {
            return factor("APPLICATION_PERIOD", "일정", "신청 일정", RecommendationFactorStatus.NEEDS_VERIFICATION,
                    "직접 확인 필요", "신청 시작일 또는 마감일 정보가 부족합니다.", today.toString(), period,
                    "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        if (today.isBefore(startDate)) {
            return factor("APPLICATION_PERIOD", "일정", "신청 일정", RecommendationFactorStatus.PARTIAL_MATCH,
                    "일정 확인 필요", "아직 신청 시작 전입니다.", today.toString(), period,
                    "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        if (today.isAfter(endDate)) {
            return factor("APPLICATION_PERIOD", "일정", "신청 일정", RecommendationFactorStatus.NOT_MATCHED,
                    "일정 확인 필요", "저장된 신청 마감일이 지났습니다.", today.toString(), period,
                    "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        long daysLeft = ChronoUnit.DAYS.between(today, endDate);
        String reason = daysLeft <= 7
                ? "현재 신청 기간이며 마감이 " + daysLeft + "일 남았습니다."
                : "현재 신청 기간 안에 있습니다.";
        return factor("APPLICATION_PERIOD", "일정", "신청 일정", RecommendationFactorStatus.STRONG_MATCH,
                "신청 가능 기간", reason, today.toString(), period, "공고 원문 확인", "OFFICIAL_NOTICE");
    }

    private RecommendationFactorResponse incomeAssetFactor(AnnouncementEligibility eligibility) {
        if (eligibility == null) {
            return factor("INCOME_ASSET", "신청 전 확인", "소득/자산 기준", RecommendationFactorStatus.UNKNOWN,
                    "정보 부족", "공고 자격조건 데이터가 부족해 자동 설명이 제한됩니다.", "프로필 입력값 참고",
                    "공고 원문 확인 필요", "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        if (StringUtils.hasText(eligibility.getIncomeAssetCriteriaRaw())) {
            return factor("INCOME_ASSET", "신청 전 확인", "소득/자산 기준",
                    RecommendationFactorStatus.NEEDS_VERIFICATION, "직접 확인 필요",
                    "소득/자산 상세 기준은 원문 대조가 필요합니다.", "프로필 입력값 참고", "공고 원문 확인 필요",
                    "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        return factor("INCOME_ASSET", "신청 전 확인", "소득/자산 기준", RecommendationFactorStatus.UNKNOWN,
                "정보 부족", "파싱된 소득/자산 상세 기준이 없습니다.", "프로필 입력값 참고", "공고 기준 없음",
                "공고 원문 확인", "OFFICIAL_NOTICE");
    }

    private RecommendationFactorResponse detailedEligibilityFactor(AnnouncementEligibility eligibility) {
        if (eligibility == null) {
            return factor("DETAILED_ELIGIBILITY", "신청 전 확인", "세부 자격조건", RecommendationFactorStatus.UNKNOWN,
                    "정보 부족", "공고 자격조건 데이터가 부족해 자동 설명이 제한됩니다.", "프로필 입력값 참고",
                    "공고 원문 확인 필요", "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        if (hasDetailedEligibilityRaw(eligibility)) {
            return factor("DETAILED_ELIGIBILITY", "신청 전 확인", "세부 자격조건",
                    RecommendationFactorStatus.NEEDS_VERIFICATION, "직접 확인 필요",
                    "우선순위, 세대원 조건, 제출서류 등 세부 자격조건은 원문 확인이 필요합니다.",
                    "프로필 입력값 참고", "공고 원문 확인 필요", "공고 원문 확인", "OFFICIAL_NOTICE");
        }
        return factor("DETAILED_ELIGIBILITY", "신청 전 확인", "세부 자격조건", RecommendationFactorStatus.UNKNOWN,
                "정보 부족", "파싱된 세부 자격조건 정보가 없습니다.", "프로필 입력값 참고", "공고 기준 없음",
                "공고 원문 확인", "OFFICIAL_NOTICE");
    }

    private boolean hasDetailedEligibilityRaw(AnnouncementEligibility eligibility) {
        return StringUtils.hasText(eligibility.getEligibilityRaw())
                || StringUtils.hasText(eligibility.getSpecialSupplyRaw())
                || StringUtils.hasText(eligibility.getMaritalRawText())
                || StringUtils.hasText(eligibility.getChildrenRawText())
                || StringUtils.hasText(eligibility.getHomelessRawText())
                || StringUtils.hasText(eligibility.getElderlyRawText());
    }

    private boolean passesEligibility(UserProfile profile, AnnouncementEligibility eligibility) {
        if (eligibility == null) {
            return true;
        }
        if (eligibility.getAgeMin() != null && profile.getAge() < eligibility.getAgeMin()) {
            return false;
        }
        if (eligibility.getAgeMax() != null && profile.getAge() > eligibility.getAgeMax()) {
            return false;
        }
        if (Boolean.TRUE.equals(eligibility.getHomelessRequired()) && !profile.isHomeless()) {
            return false;
        }
        if (Boolean.TRUE.equals(eligibility.getLowIncomeRequired())
                && !(profile.isLowIncome() || profile.isRecipient() || profile.isNearPoverty())) {
            return false;
        }
        if (Boolean.TRUE.equals(eligibility.getElderlyRequired()) && profile.getAge() < defaultElderlyAge(eligibility)) {
            return false;
        }
        if (eligibility.getChildrenMinCount() != null && profile.getChildrenCount() < eligibility.getChildrenMinCount()) {
            return false;
        }
        return passesMaritalCondition(profile, eligibility);
    }

    private boolean passesMaritalCondition(UserProfile profile, AnnouncementEligibility eligibility) {
        MaritalTargetType maritalTargetType = eligibility.getMaritalTargetType();
        if (maritalTargetType == null || maritalTargetType == MaritalTargetType.ANY) {
            return true;
        }

        return switch (maritalTargetType) {
            case SINGLE -> profile.getMaritalStatus() == MaritalStatus.SINGLE;
            case MARRIED -> profile.getMaritalStatus() == MaritalStatus.MARRIED;
            case ENGAGED -> profile.getMaritalStatus() != MaritalStatus.MARRIED;
            case NEWLYWED -> profile.getMaritalStatus() == MaritalStatus.MARRIED
                    && (profile.getMarriageYears() == null
                    || eligibility.getMarriageYearLimit() == null
                    || profile.getMarriageYears() <= eligibility.getMarriageYearLimit());
            case ANY -> true;
        };
    }

    private boolean matchesBudget(UserProfile profile, Announcement announcement) {
        if (profile.getMaxDeposit() != null && announcement.getDepositAmount() != null
                && announcement.getDepositAmount() > profile.getMaxDeposit()) {
            return false;
        }
        return profile.getMaxMonthlyRent() == null
                || announcement.getMonthlyRentAmount() == null
                || announcement.getMonthlyRentAmount() <= profile.getMaxMonthlyRent();
    }

    private int defaultElderlyAge(AnnouncementEligibility eligibility) {
        return eligibility.getElderlyAgeMin() != null ? eligibility.getElderlyAgeMin() : 65;
    }

    private RecommendationFactorResponse factor(String key, String group, String label, RecommendationFactorStatus status,
                                                String scoreImpactLabel, String reason, String userValue,
                                                String announcementValue, String actionLabel, String actionTarget) {
        return new RecommendationFactorResponse(key, group, label, status, scoreImpactLabel, reason, userValue,
                announcementValue, actionLabel, actionTarget);
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return StringUtils.hasText(left) && StringUtils.hasText(right) && left.equalsIgnoreCase(right);
    }

    private String resolveHouseType(Announcement announcement) {
        if (StringUtils.hasText(announcement.getHouseTypeRaw())) {
            String normalizedFromRaw = announcementNormalizer.normalizeHouseType(announcement.getHouseTypeRaw());
            if (StringUtils.hasText(normalizedFromRaw)) {
                return normalizedFromRaw;
            }
        }

        return StringUtils.hasText(announcement.getHouseTypeNormalized()) ? announcement.getHouseTypeNormalized() : null;
    }

    private String resolveRegionLevel2(Announcement announcement) {
        if (StringUtils.hasText(announcement.getRegionLevel2())) {
            String extracted = extractRegionLevel2Token(announcement.getRegionLevel2(), announcement.getRegionLevel1());
            if (StringUtils.hasText(extracted)) {
                return extracted;
            }
        }

        return extractRegionLevel2Token(announcement.getFullAddress(), announcement.getRegionLevel1());
    }

    private String extractRegionLevel2Token(String source, String regionLevel1) {
        if (!StringUtils.hasText(source)) {
            return null;
        }

        String normalizedSource = normalizeWhitespace(source);
        String[] tokens = normalizedSource.split(" ");
        int startIndex = 0;

        if (StringUtils.hasText(regionLevel1)) {
            String normalizedRegionLevel1 = normalizeRegionToken(regionLevel1);
            for (int index = 0; index < tokens.length; index++) {
                if (normalizedRegionLevel1.equals(normalizeRegionToken(tokens[index]))) {
                    startIndex = index + 1;
                    break;
                }
            }
        }

        for (int index = startIndex; index < tokens.length; index++) {
            String token = sanitizeRegionToken(tokens[index]);
            if (isLevel2RegionToken(token)) {
                return token;
            }
        }

        String fallback = sanitizeRegionToken(normalizedSource);
        return isLevel2RegionToken(fallback) ? fallback : null;
    }

    private String normalizeRegionToken(String value) {
        return sanitizeRegionToken(value).toLowerCase();
    }

    private String sanitizeRegionToken(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return normalizeWhitespace(value).replaceAll("^[\\p{Punct}]+|[\\p{Punct}]+$", "");
    }

    private String normalizeWhitespace(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private boolean isLevel2RegionToken(String token) {
        return StringUtils.hasText(token)
                && (token.endsWith("시") || token.endsWith("군") || token.endsWith("구"));
    }

    private String categoriesValue(Set<CategoryCode> categories) {
        if (categories.isEmpty()) {
            return "정보 없음";
        }
        return categories.stream().map(Enum::name).sorted().collect(Collectors.joining(", "));
    }

    private String displayValue(String value) {
        return StringUtils.hasText(value) ? value : "정보 없음";
    }

    private String moneyValue(Long amount) {
        return amount != null ? amount + "만원" : "정보 없음";
    }

    private String periodValue(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return "공고 원문 확인 필요";
        }
        return startDate + " ~ " + endDate;
    }
}
