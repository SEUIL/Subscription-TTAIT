package com.ttait.subscription.announcement.service;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementCategory;
import com.ttait.subscription.announcement.domain.AnnouncementEligibility;
import com.ttait.subscription.announcement.domain.ParseReviewStatus;
import com.ttait.subscription.announcement.dto.RecommendationItemResponse;
import com.ttait.subscription.announcement.repository.AnnouncementCategoryRepository;
import com.ttait.subscription.announcement.repository.AnnouncementEligibilityRepository;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.user.domain.UserCategory;
import com.ttait.subscription.user.domain.UserProfile;
import com.ttait.subscription.user.domain.enums.CategoryCode;
import com.ttait.subscription.user.repository.UserCategoryRepository;
import com.ttait.subscription.user.repository.UserProfileRepository;
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

@Service
@Transactional(readOnly = true)
public class RecommendationService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementCategoryRepository announcementCategoryRepository;
    private final AnnouncementEligibilityRepository announcementEligibilityRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final RecommendationMatchEvaluator recommendationMatchEvaluator;

    public RecommendationService(AnnouncementRepository announcementRepository,
                                 AnnouncementCategoryRepository announcementCategoryRepository,
                                 AnnouncementEligibilityRepository announcementEligibilityRepository,
                                 UserProfileRepository userProfileRepository,
                                 UserCategoryRepository userCategoryRepository,
                                 RecommendationMatchEvaluator recommendationMatchEvaluator) {
        this.announcementRepository = announcementRepository;
        this.announcementCategoryRepository = announcementCategoryRepository;
        this.announcementEligibilityRepository = announcementEligibilityRepository;
        this.userProfileRepository = userProfileRepository;
        this.userCategoryRepository = userCategoryRepository;
        this.recommendationMatchEvaluator = recommendationMatchEvaluator;
    }

    public Page<RecommendationItemResponse> getRecommendations(Long userId, Pageable pageable) {
        UserProfile profile = userProfileRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "profile setup required"));

        Set<CategoryCode> userCategories = recommendationMatchEvaluator.deriveUserCategories(profile,
                userCategoryRepository.findByUserId(userId).stream().map(UserCategory::getCategoryCode).toList());

        List<Announcement> announcements = announcementRepository.findPublicVisible(
                        ParseReviewStatus.publicVisibleStatuses(),
                        Pageable.unpaged())
                .getContent();
        List<Long> announcementIds = announcements.stream().map(Announcement::getId).toList();
        Map<Long, Set<CategoryCode>> announcementCategoryMap = loadAnnouncementCategoryMap(announcementIds);
        Map<Long, AnnouncementEligibility> eligibilityMap = loadEligibilityMap(announcementIds);

        List<RecommendationItemResponse> recommendations = announcements.stream()
                .map(announcement -> recommendationMatchEvaluator.evaluate(
                        announcement,
                        profile,
                        userCategories,
                        announcementCategoryMap.getOrDefault(announcement.getId(), EnumSet.noneOf(CategoryCode.class)),
                        eligibilityMap.get(announcement.getId())))
                .filter(RecommendationEvaluationResult::recommended)
                .map(RecommendationEvaluationResult::item)
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
}
