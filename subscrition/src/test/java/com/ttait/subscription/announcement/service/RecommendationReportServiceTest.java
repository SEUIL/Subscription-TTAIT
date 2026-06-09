package com.ttait.subscription.announcement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementCategory;
import com.ttait.subscription.announcement.domain.AnnouncementEligibility;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.domain.MatchSource;
import com.ttait.subscription.announcement.domain.ParseReviewStatus;
import com.ttait.subscription.announcement.domain.SourceType;
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
import com.ttait.subscription.user.domain.enums.MaritalStatus;
import com.ttait.subscription.user.repository.UserCategoryRepository;
import com.ttait.subscription.user.repository.UserProfileRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RecommendationReportServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserCategoryRepository userCategoryRepository;

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private AnnouncementCategoryRepository announcementCategoryRepository;

    @Mock
    private AnnouncementEligibilityRepository announcementEligibilityRepository;

    @Mock
    private RecommendationMatchEvaluator recommendationMatchEvaluator;

    private RecommendationReportService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationReportService(
                userProfileRepository,
                userCategoryRepository,
                announcementRepository,
                announcementCategoryRepository,
                announcementEligibilityRepository,
                recommendationMatchEvaluator);
    }

    @Test
    void getReportThrowsBadRequestWhenProfileIsMissing() {
        given(userProfileRepository.findByUserIdAndDeletedFalse(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReport(10L, 1L))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getMessage()).isEqualTo("profile setup required");
                });

        then(announcementRepository).should(never()).findPublicVisibleById(1L, publicVisibleReviewStatuses());
        then(recommendationMatchEvaluator).shouldHaveNoInteractions();
    }

    @Test
    void getReportThrowsNotFoundWhenAnnouncementIsNotPublicVisible() {
        given(userProfileRepository.findByUserIdAndDeletedFalse(10L)).willReturn(Optional.of(profile()));
        given(announcementRepository.findPublicVisibleById(1L, publicVisibleReviewStatuses())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReport(10L, 1L))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getMessage()).isEqualTo("announcement not found");
                });

        then(announcementEligibilityRepository).shouldHaveNoInteractions();
        then(recommendationMatchEvaluator).shouldHaveNoInteractions();
    }

    @Test
    void getReportReturnsReportForRecommendedAnnouncement() {
        UserProfile profile = profile();
        Announcement announcement = announcement();
        AnnouncementEligibility eligibility = AnnouncementEligibility.builder()
                .incomeAssetCriteriaRaw("raw income criteria")
                .build();
        UserCategory userCategory = UserCategory.builder().categoryCode(CategoryCode.YOUTH).build();
        AnnouncementCategory announcementCategory = AnnouncementCategory.builder()
                .announcement(announcement)
                .categoryCode(CategoryCode.YOUTH)
                .matchSource(MatchSource.RULE)
                .build();
        RecommendationEvaluationResult evaluation = new RecommendationEvaluationResult(
                true,
                item(),
                factors());
        given(userProfileRepository.findByUserIdAndDeletedFalse(10L)).willReturn(Optional.of(profile));
        given(announcementRepository.findPublicVisibleById(1L, publicVisibleReviewStatuses())).willReturn(Optional.of(announcement));
        given(announcementEligibilityRepository.findByAnnouncementId(1L)).willReturn(Optional.of(eligibility));
        given(userCategoryRepository.findByUserId(10L)).willReturn(List.of(userCategory));
        given(recommendationMatchEvaluator.deriveUserCategories(profile, List.of(CategoryCode.YOUTH)))
                .willReturn(EnumSet.of(CategoryCode.YOUTH));
        given(announcementCategoryRepository.findByAnnouncementIdIn(List.of(1L))).willReturn(List.of(announcementCategory));
        given(recommendationMatchEvaluator.evaluate(
                announcement,
                profile,
                EnumSet.of(CategoryCode.YOUTH),
                EnumSet.of(CategoryCode.YOUTH),
                eligibility)).willReturn(evaluation);

        RecommendationReportResponse response = service.getReport(10L, 1L);

        assertThat(response.announcementId()).isEqualTo(1L);
        assertThat(response.matchScore()).isEqualTo(88);
        assertThat(response.summaryStatus()).isEqualTo(RecommendationReportSummaryStatus.RECOMMENDED_WITH_CHECKS);
        assertThat(response.factorCounts()).isEqualTo(new RecommendationFactorCountsResponse(1, 0, 2, 0, 0));
        assertThat(response.existingMatchReasons()).containsExactly("희망 지역과 일치");
        assertThat(response.disclaimer()).isEqualTo(RecommendationReportService.DISCLAIMER);
    }

    @Test
    void getReportThrowsNotFoundWhenAnnouncementIsPublicButNotRecommendedForUser() {
        UserProfile profile = profile();
        Announcement announcement = announcement();
        given(userProfileRepository.findByUserIdAndDeletedFalse(10L)).willReturn(Optional.of(profile));
        given(announcementRepository.findPublicVisibleById(1L, publicVisibleReviewStatuses())).willReturn(Optional.of(announcement));
        given(announcementEligibilityRepository.findByAnnouncementId(1L)).willReturn(Optional.empty());
        given(userCategoryRepository.findByUserId(10L)).willReturn(List.of());
        given(recommendationMatchEvaluator.deriveUserCategories(profile, List.of()))
                .willReturn(EnumSet.noneOf(CategoryCode.class));
        given(announcementCategoryRepository.findByAnnouncementIdIn(List.of(1L))).willReturn(List.of());
        given(recommendationMatchEvaluator.evaluate(
                announcement,
                profile,
                EnumSet.noneOf(CategoryCode.class),
                EnumSet.noneOf(CategoryCode.class),
                null)).willReturn(RecommendationEvaluationResult.notRecommended(List.of()));

        assertThatThrownBy(() -> service.getReport(10L, 1L))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getMessage()).isEqualTo("recommendation not found");
                });
    }

    @Test
    void responseDtosDoNotExposeRawFieldNames() {
        assertThat(componentNames(RecommendationReportResponse.class)).doesNotContain(
                "eligibilityRaw",
                "specialSupplyRaw",
                "incomeAssetCriteriaRaw");
        assertThat(componentNames(RecommendationFactorResponse.class)).doesNotContain(
                "eligibilityRaw",
                "specialSupplyRaw",
                "incomeAssetCriteriaRaw");
    }

    private List<ParseReviewStatus> publicVisibleReviewStatuses() {
        return List.of(ParseReviewStatus.APPROVED, ParseReviewStatus.CORRECTED);
    }

    private RecommendationItemResponse item() {
        return new RecommendationItemResponse(
                1L,
                "sample notice",
                "LH",
                "Public Rental",
                "Apartment",
                "Gyeonggi",
                "Suwon",
                "Gyeonggi Suwon Sample-ro 1",
                "Sample Complex",
                5000L,
                30L,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 10),
                "OPEN",
                "https://example.com/PAN-001",
                88,
                List.of("희망 지역과 일치"));
    }

    private List<RecommendationFactorResponse> factors() {
        return List.of(
                new RecommendationFactorResponse(
                        "REGION",
                        "선호 조건",
                        "지역",
                        RecommendationFactorStatus.STRONG_MATCH,
                        "추천 점수에 긍정적",
                        "선호 지역과 공고 지역이 일치합니다.",
                        "Gyeonggi",
                        "Gyeonggi",
                        null,
                        "NONE"),
                new RecommendationFactorResponse(
                        "INCOME_ASSET",
                        "신청 전 확인",
                        "소득/자산 기준",
                        RecommendationFactorStatus.NEEDS_VERIFICATION,
                        "직접 확인 필요",
                        "소득/자산 상세 기준은 원문 대조가 필요합니다.",
                        "프로필 입력값 참고",
                        "공고 원문 확인 필요",
                        "공고 원문 확인",
                        "OFFICIAL_NOTICE"),
                new RecommendationFactorResponse(
                        "DETAILED_ELIGIBILITY",
                        "신청 전 확인",
                        "세부 자격조건",
                        RecommendationFactorStatus.NEEDS_VERIFICATION,
                        "직접 확인 필요",
                        "우선순위, 세대원 조건, 제출서류 등 세부 자격조건은 원문 확인이 필요합니다.",
                        "프로필 입력값 참고",
                        "공고 원문 확인 필요",
                        "공고 원문 확인",
                        "OFFICIAL_NOTICE"));
    }

    private UserProfile profile() {
        return UserProfile.builder()
                .age(30)
                .maritalStatus(MaritalStatus.MARRIED)
                .marriageYears(3)
                .childrenCount(1)
                .homeless(true)
                .lowIncome(false)
                .elderly(false)
                .recipient(false)
                .nearPoverty(false)
                .singleParentFamily(false)
                .maxDeposit(5000L)
                .maxMonthlyRent(30L)
                .build();
    }

    private Announcement announcement() {
        Announcement announcement = Announcement.builder()
                .sourcePrimary(SourceType.LH)
                .sourceNoticeId("PAN-001")
                .noticeName("sample notice")
                .providerName("LH")
                .sourceNoticeUrl("https://example.com/PAN-001")
                .noticeStatus(AnnouncementStatus.OPEN)
                .announcementDate(LocalDate.of(2026, 5, 1))
                .applicationStartDate(LocalDate.of(2026, 6, 1))
                .applicationEndDate(LocalDate.of(2026, 6, 10))
                .winnerAnnouncementDate(LocalDate.of(2026, 7, 1))
                .regionLevel1("Gyeonggi")
                .regionLevel2("Suwon")
                .fullAddress("Gyeonggi Suwon Sample-ro 1")
                .complexName("Sample Complex")
                .supplyTypeNormalized("Public Rental")
                .houseTypeNormalized("Apartment")
                .depositAmount(5000L)
                .monthlyRentAmount(30L)
                .supplyHouseholdCount(30)
                .matchKey("match-key")
                .merged(false)
                .collectedAt(LocalDateTime.of(2026, 5, 1, 0, 0))
                .build();
        ReflectionTestUtils.setField(announcement, "id", 1L);
        return announcement;
    }

    private String[] componentNames(Class<? extends Record> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
                .map(component -> component.getName())
                .toArray(String[]::new);
    }
}
