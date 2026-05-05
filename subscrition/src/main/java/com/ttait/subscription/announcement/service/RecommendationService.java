package com.ttait.subscription.announcement.service;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementCategory;
import com.ttait.subscription.announcement.domain.AnnouncementEligibility;
import com.ttait.subscription.announcement.domain.MaritalTargetType;
import com.ttait.subscription.announcement.dto.RecommendationItemResponse;
import com.ttait.subscription.announcement.repository.AnnouncementCategoryRepository;
import com.ttait.subscription.announcement.repository.AnnouncementEligibilityRepository;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.user.domain.UserCategory;
import com.ttait.subscription.user.domain.UserProfile;
import com.ttait.subscription.user.domain.enums.CategoryCode;
import com.ttait.subscription.user.domain.enums.MaritalStatus;
import com.ttait.subscription.user.repository.UserCategoryRepository;
import com.ttait.subscription.user.repository.UserProfileRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class RecommendationService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementCategoryRepository announcementCategoryRepository;
    private final AnnouncementEligibilityRepository announcementEligibilityRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserCategoryRepository userCategoryRepository;

    public RecommendationService(AnnouncementRepository announcementRepository,
                                 AnnouncementCategoryRepository announcementCategoryRepository,
                                 AnnouncementEligibilityRepository announcementEligibilityRepository,
                                 UserProfileRepository userProfileRepository,
                                 UserCategoryRepository userCategoryRepository) {
        this.announcementRepository = announcementRepository;
        this.announcementCategoryRepository = announcementCategoryRepository;
        this.announcementEligibilityRepository = announcementEligibilityRepository;
        this.userProfileRepository = userProfileRepository;
        this.userCategoryRepository = userCategoryRepository;
    }

    public Page<RecommendationItemResponse> getRecommendations(Long userId, Pageable pageable) {
        UserProfile profile = userProfileRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "profile setup required"));

        Set<CategoryCode> userCategories = deriveUserCategories(profile,
                userCategoryRepository.findByUserId(userId).stream().map(UserCategory::getCategoryCode).toList());

        List<Announcement> announcements = announcementRepository.findByDeletedFalseAndMergedFalse(Pageable.unpaged())
                .getContent();
        List<Long> announcementIds = announcements.stream().map(Announcement::getId).toList();
        Map<Long, Set<CategoryCode>> announcementCategoryMap = loadAnnouncementCategoryMap(announcementIds);
        Map<Long, AnnouncementEligibility> eligibilityMap = loadEligibilityMap(announcementIds);

        List<RecommendationItemResponse> recommendations = announcements.stream()
                .map(announcement -> scoreAnnouncement(
                        announcement,
                        profile,
                        userCategories,
                        announcementCategoryMap.getOrDefault(announcement.getId(), EnumSet.noneOf(CategoryCode.class)),
                        eligibilityMap.get(announcement.getId())))
                .filter(candidate -> candidate != null)
                .sorted(Comparator
                        .comparingInt(RecommendationItemResponse::matchScore).reversed()
                        .thenComparing(RecommendationItemResponse::applicationEndDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(RecommendationItemResponse::announcementId, Comparator.reverseOrder()))
                .toList();

        int start = (int) pageable.getOffset();
        if (start >= recommendations.size()) {
            return new PageImpl<>(List.of(), pageable, recommendations.size());
        }

        int end = Math.min(start + pageable.getPageSize(), recommendations.size());
        return new PageImpl<>(recommendations.subList(start, end), pageable, recommendations.size());
    }

    private RecommendationItemResponse scoreAnnouncement(Announcement announcement, UserProfile profile,
                                                         Set<CategoryCode> userCategories,
                                                         Set<CategoryCode> announcementCategories,
                                                         AnnouncementEligibility eligibility) {
        if (!passesEligibility(profile, eligibility)) {
            return null;
        }
        if (!matchesBudget(profile, announcement)) {
            return null;
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
        if (equalsIgnoreCase(profile.getPreferredRegionLevel2(), announcement.getRegionLevel2())) {
            score += 12;
            reasons.add("세부 희망 지역과 일치");
        }
        if (equalsIgnoreCase(profile.getPreferredHouseType(), announcement.getHouseTypeNormalized())) {
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
            return null;
        }

        return new RecommendationItemResponse(
                announcement.getId(),
                announcement.getNoticeName(),
                announcement.getProviderName(),
                announcement.getSupplyTypeNormalized(),
                announcement.getHouseTypeNormalized(),
                announcement.getRegionLevel1(),
                announcement.getRegionLevel2(),
                announcement.getFullAddress(),
                announcement.getComplexName(),
                announcement.getDepositAmount(),
                announcement.getMonthlyRentAmount(),
                announcement.getApplicationStartDate(),
                announcement.getApplicationEndDate(),
                announcement.getNoticeStatus().name(),
                announcement.getSourceNoticeUrl(),
                score,
                reasons
        );
    }

    private Set<CategoryCode> deriveUserCategories(UserProfile profile, List<CategoryCode> selectedCategories) {
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

    private Map<Long, Set<CategoryCode>> loadAnnouncementCategoryMap(Collection<Long> announcementIds) {
        Map<Long, Set<CategoryCode>> categoryMap = new HashMap<>();
        if (announcementIds.isEmpty()) {
            return categoryMap;
        }

        for (AnnouncementCategory category : announcementCategoryRepository.findByAnnouncementIdIn(announcementIds)) {
            categoryMap.computeIfAbsent(category.getAnnouncement().getId(), ignored -> EnumSet.noneOf(CategoryCode.class))
                    .add(category.getCategoryCode());
        }
        return categoryMap;
    }

    private Map<Long, AnnouncementEligibility> loadEligibilityMap(Collection<Long> announcementIds) {
        Map<Long, AnnouncementEligibility> map = new HashMap<>();
        if (announcementIds.isEmpty()) {
            return map;
        }

        for (AnnouncementEligibility eligibility : announcementEligibilityRepository.findByAnnouncementIdIn(announcementIds)) {
            map.put(eligibility.getAnnouncement().getId(), eligibility);
        }
        return map;
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

    private boolean equalsIgnoreCase(String left, String right) {
        return StringUtils.hasText(left) && StringUtils.hasText(right) && left.equalsIgnoreCase(right);
    }
}
