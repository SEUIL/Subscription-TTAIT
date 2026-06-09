package com.ttait.subscription.announcement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementEligibility;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.domain.SourceType;
import com.ttait.subscription.announcement.dto.RecommendationFactorResponse;
import com.ttait.subscription.announcement.dto.RecommendationFactorStatus;
import com.ttait.subscription.external.support.AnnouncementNormalizer;
import com.ttait.subscription.user.domain.UserProfile;
import com.ttait.subscription.user.domain.enums.CategoryCode;
import com.ttait.subscription.user.domain.enums.MaritalStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RecommendationMatchEvaluatorTest {

    @Mock
    private AnnouncementNormalizer announcementNormalizer;

    private RecommendationMatchEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new RecommendationMatchEvaluator(
                announcementNormalizer,
                Clock.fixed(Instant.parse("2026-06-09T00:00:00Z"), ZoneId.of("UTC")));
    }

    @Test
    void evaluatePreservesRecommendationScoreAndReasons() {
        UserProfile profile = profileBuilder()
                .age(65)
                .preferredRegionLevel1("Gyeonggi")
                .preferredRegionLevel2("수원시")
                .preferredHouseType("Apartment")
                .preferredSupplyType("Public Rental")
                .build();
        Announcement announcement = announcementBuilder().build();
        ReflectionTestUtils.setField(announcement, "id", 1L);
        AnnouncementEligibility eligibility = AnnouncementEligibility.builder()
                .homelessRequired(true)
                .childrenMinCount(1)
                .elderlyRequired(true)
                .build();

        RecommendationEvaluationResult result = evaluator.evaluate(
                announcement,
                profile,
                EnumSet.of(CategoryCode.ELDERLY),
                EnumSet.of(CategoryCode.ELDERLY),
                eligibility);

        assertThat(result.recommended()).isTrue();
        assertThat(result.item().matchScore()).isEqualTo(125);
        assertThat(result.item().matchReasons()).containsExactly(
                "선택한 신분 유형과 일치",
                "희망 지역과 일치",
                "세부 희망 지역과 일치",
                "희망 주택 유형과 일치",
                "희망 공급 유형과 일치",
                "보증금 예산 범위 충족",
                "월세 예산 범위 충족",
                "무주택 조건 충족",
                "자녀 수 조건 충족",
                "고령자 조건 충족");
        assertThat(factor(result, "REGION").status()).isEqualTo(RecommendationFactorStatus.STRONG_MATCH);
        assertThat(factor(result, "INCOME_ASSET").status()).isEqualTo(RecommendationFactorStatus.UNKNOWN);
        assertThat(factor(result, "DETAILED_ELIGIBILITY").status()).isEqualTo(RecommendationFactorStatus.UNKNOWN);
    }

    @Test
    void evaluateUsesNormalizedHouseTypeFromRawWhenPresent() {
        given(announcementNormalizer.normalizeHouseType("raw house type")).willReturn("Apartment");
        Announcement announcement = announcementBuilder()
                .houseTypeRaw("raw house type")
                .houseTypeNormalized("Fallback")
                .build();
        ReflectionTestUtils.setField(announcement, "id", 1L);

        RecommendationEvaluationResult result = evaluator.evaluate(
                announcement,
                profileBuilder().preferredHouseType("Apartment").build(),
                EnumSet.noneOf(CategoryCode.class),
                EnumSet.noneOf(CategoryCode.class),
                AnnouncementEligibility.builder().build());

        assertThat(result.recommended()).isTrue();
        assertThat(result.item().houseType()).isEqualTo("Apartment");
        assertThat(result.item().matchReasons()).contains("희망 주택 유형과 일치");
    }

    @Test
    void evaluateReturnsNotRecommendedWhenBudgetExceedsProfileMax() {
        Announcement announcement = announcementBuilder().depositAmount(6000L).build();
        ReflectionTestUtils.setField(announcement, "id", 1L);

        RecommendationEvaluationResult result = evaluator.evaluate(
                announcement,
                profileBuilder().maxDeposit(5000L).build(),
                EnumSet.noneOf(CategoryCode.class),
                EnumSet.noneOf(CategoryCode.class),
                AnnouncementEligibility.builder().build());

        assertThat(result.recommended()).isFalse();
        assertThat(result.item()).isNull();
        assertThat(factor(result, "DEPOSIT_BUDGET").status()).isEqualTo(RecommendationFactorStatus.NOT_MATCHED);
    }

    @Test
    void incomeAssetFactorNeedsVerificationWithoutExposingRawText() {
        Announcement announcement = announcementBuilder().build();
        ReflectionTestUtils.setField(announcement, "id", 1L);

        RecommendationEvaluationResult result = evaluator.evaluate(
                announcement,
                profileBuilder().preferredRegionLevel1("Gyeonggi").build(),
                EnumSet.noneOf(CategoryCode.class),
                EnumSet.noneOf(CategoryCode.class),
                AnnouncementEligibility.builder().incomeAssetCriteriaRaw("월평균소득 70% 이하 원문").build());

        RecommendationFactorResponse factor = factor(result, "INCOME_ASSET");
        assertThat(factor.status()).isEqualTo(RecommendationFactorStatus.NEEDS_VERIFICATION);
        assertThat(factor.reason()).doesNotContain("월평균소득 70% 이하 원문");
        assertThat(factor.announcementValue()).doesNotContain("월평균소득 70% 이하 원문");
    }

    @Test
    void detailedEligibilityFactorNeedsVerificationWithoutExposingRawText() {
        Announcement announcement = announcementBuilder().build();
        ReflectionTestUtils.setField(announcement, "id", 1L);

        RecommendationEvaluationResult result = evaluator.evaluate(
                announcement,
                profileBuilder().preferredRegionLevel1("Gyeonggi").build(),
                EnumSet.noneOf(CategoryCode.class),
                EnumSet.noneOf(CategoryCode.class),
                AnnouncementEligibility.builder()
                        .eligibilityRaw("우선순위 및 세대원 조건 원문")
                        .specialSupplyRaw("제출서류 원문")
                        .build());

        RecommendationFactorResponse factor = factor(result, "DETAILED_ELIGIBILITY");
        assertThat(factor.status()).isEqualTo(RecommendationFactorStatus.NEEDS_VERIFICATION);
        assertThat(factor.reason()).doesNotContain("우선순위 및 세대원 조건 원문");
        assertThat(factor.announcementValue()).doesNotContain("제출서류 원문");
    }

    private RecommendationFactorResponse factor(RecommendationEvaluationResult result, String key) {
        return result.factors().stream()
                .filter(factor -> factor.key().equals(key))
                .findFirst()
                .orElseThrow();
    }

    private UserProfile.UserProfileBuilder profileBuilder() {
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
                .preferredRegionLevel1("Seoul")
                .preferredRegionLevel2("Gangnam")
                .preferredHouseType("Villa")
                .preferredSupplyType("Public Rental")
                .maxDeposit(5000L)
                .maxMonthlyRent(30L);
    }

    private Announcement.AnnouncementBuilder announcementBuilder() {
        return Announcement.builder()
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
                .regionLevel2("수원시")
                .fullAddress("Gyeonggi Suwon Sample-ro 1")
                .complexName("Sample Complex")
                .supplyTypeNormalized("Public Rental")
                .houseTypeNormalized("Apartment")
                .depositAmount(5000L)
                .monthlyRentAmount(30L)
                .supplyHouseholdCount(30)
                .matchKey("match-key")
                .merged(false)
                .collectedAt(LocalDateTime.of(2026, 5, 1, 0, 0));
    }
}
