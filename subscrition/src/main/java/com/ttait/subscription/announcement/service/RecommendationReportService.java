package com.ttait.subscription.announcement.service;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementCategory;
import com.ttait.subscription.announcement.domain.AnnouncementEligibility;
import com.ttait.subscription.announcement.domain.ParseReviewStatus;
import com.ttait.subscription.announcement.dto.RecommendationFactorCountsResponse;
import com.ttait.subscription.announcement.dto.RecommendationFactorResponse;
import com.ttait.subscription.announcement.dto.RecommendationFactorStatus;
import com.ttait.subscription.announcement.dto.RecommendationItemResponse;
import com.ttait.subscription.announcement.dto.RecommendationReportResponse;
import com.ttait.subscription.announcement.dto.RecommendationReportSummaryStatus;
import com.ttait.subscription.announcement.repository.AnnouncementCategoryRepository;
import com.ttait.subscription.announcement.repository.AnnouncementEligibilityRepository;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.user.domain.UserCategory;
import com.ttait.subscription.user.domain.UserProfile;
import com.ttait.subscription.user.domain.enums.CategoryCode;
import com.ttait.subscription.user.repository.UserCategoryRepository;
import com.ttait.subscription.user.repository.UserProfileRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RecommendationReportService {

    static final String DISCLAIMER = "이 리포트는 저장된 프로필과 공고 데이터를 기준으로 추천 결과를 설명하는 참고 자료입니다. 최종 신청 가능 여부와 세부 자격은 공고 원문과 신청 사이트에서 반드시 확인해야 합니다.";

    private final UserProfileRepository userProfileRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final AnnouncementRepository announcementRepository;
    private final AnnouncementCategoryRepository announcementCategoryRepository;
    private final AnnouncementEligibilityRepository announcementEligibilityRepository;
    private final RecommendationMatchEvaluator recommendationMatchEvaluator;

    public RecommendationReportService(UserProfileRepository userProfileRepository,
                                       UserCategoryRepository userCategoryRepository,
                                       AnnouncementRepository announcementRepository,
                                       AnnouncementCategoryRepository announcementCategoryRepository,
                                       AnnouncementEligibilityRepository announcementEligibilityRepository,
                                       RecommendationMatchEvaluator recommendationMatchEvaluator) {
        this.userProfileRepository = userProfileRepository;
        this.userCategoryRepository = userCategoryRepository;
        this.announcementRepository = announcementRepository;
        this.announcementCategoryRepository = announcementCategoryRepository;
        this.announcementEligibilityRepository = announcementEligibilityRepository;
        this.recommendationMatchEvaluator = recommendationMatchEvaluator;
    }

    public RecommendationReportResponse getReport(Long userId, Long announcementId) {
        UserProfile profile = userProfileRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "profile setup required"));
        Announcement announcement = announcementRepository.findPublicVisibleById(announcementId, ParseReviewStatus.publicVisibleStatuses())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "announcement not found"));
        AnnouncementEligibility eligibility = announcementEligibilityRepository.findByAnnouncementId(announcementId)
                .orElse(null);

        Set<CategoryCode> userCategories = recommendationMatchEvaluator.deriveUserCategories(profile,
                userCategoryRepository.findByUserId(userId).stream().map(UserCategory::getCategoryCode).toList());
        Set<CategoryCode> announcementCategories = loadAnnouncementCategories(announcementId);
        RecommendationEvaluationResult result = recommendationMatchEvaluator.evaluate(
                announcement,
                profile,
                userCategories,
                announcementCategories,
                eligibility);

        if (!result.recommended()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "recommendation not found");
        }

        RecommendationItemResponse item = result.item();
        RecommendationFactorCountsResponse factorCounts = factorCounts(result.factors());
        RecommendationReportSummaryStatus summaryStatus = summaryStatus(item.matchReasons(), factorCounts);
        return new RecommendationReportResponse(
                item.announcementId(),
                item.noticeName(),
                item.providerName(),
                item.matchScore(),
                summaryStatus,
                summaryMessage(summaryStatus),
                item.matchReasons(),
                factorCounts,
                result.factors(),
                item.sourceNoticeUrl(),
                DISCLAIMER
        );
    }

    private Set<CategoryCode> loadAnnouncementCategories(Long announcementId) {
        Set<CategoryCode> categories = EnumSet.noneOf(CategoryCode.class);
        for (AnnouncementCategory category : announcementCategoryRepository.findByAnnouncementIdIn(List.of(announcementId))) {
            categories.add(category.getCategoryCode());
        }
        return categories;
    }

    private RecommendationFactorCountsResponse factorCounts(List<RecommendationFactorResponse> factors) {
        return new RecommendationFactorCountsResponse(
                count(factors, RecommendationFactorStatus.STRONG_MATCH),
                count(factors, RecommendationFactorStatus.PARTIAL_MATCH),
                count(factors, RecommendationFactorStatus.NEEDS_VERIFICATION),
                count(factors, RecommendationFactorStatus.NOT_MATCHED),
                count(factors, RecommendationFactorStatus.UNKNOWN)
        );
    }

    private long count(List<RecommendationFactorResponse> factors, RecommendationFactorStatus status) {
        return factors.stream()
                .filter(factor -> factor.status() == status)
                .count();
    }

    private RecommendationReportSummaryStatus summaryStatus(List<String> matchReasons,
                                                            RecommendationFactorCountsResponse factorCounts) {
        if (matchReasons.isEmpty() || factorCounts.unknown() >= 3) {
            return RecommendationReportSummaryStatus.LOW_EXPLANATION_CONFIDENCE;
        }
        if (factorCounts.needsVerification() > 0) {
            return RecommendationReportSummaryStatus.RECOMMENDED_WITH_CHECKS;
        }
        if (factorCounts.notMatched() > 0 || factorCounts.partialMatch() > 0) {
            return RecommendationReportSummaryStatus.MIXED_MATCH;
        }
        return RecommendationReportSummaryStatus.HIGHLY_RECOMMENDED;
    }

    private String summaryMessage(RecommendationReportSummaryStatus status) {
        return switch (status) {
            case HIGHLY_RECOMMENDED -> "내 조건과 잘 맞는 추천입니다.";
            case RECOMMENDED_WITH_CHECKS -> "추천되지만 확인할 조건이 있습니다.";
            case LOW_EXPLANATION_CONFIDENCE -> "추천 사유 설명이 제한적입니다.";
            case MIXED_MATCH -> "일부 조건만 맞습니다.";
        };
    }
}
