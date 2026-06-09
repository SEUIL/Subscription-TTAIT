package com.ttait.subscription.announcement.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementEligibility;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.domain.MaritalTargetType;
import com.ttait.subscription.announcement.domain.SourceType;
import com.ttait.subscription.announcement.dto.EligibilityCheckStatus;
import com.ttait.subscription.announcement.dto.EligibilityChecklistItemResponse;
import com.ttait.subscription.announcement.dto.EligibilityChecklistResponse;
import com.ttait.subscription.announcement.dto.EligibilitySummaryStatus;
import com.ttait.subscription.user.domain.UserProfile;
import com.ttait.subscription.user.domain.enums.MaritalStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EligibilityCheckEvaluatorTest {

    private EligibilityCheckEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new EligibilityCheckEvaluator(Clock.fixed(
                Instant.parse("2026-06-09T00:00:00Z"),
                ZoneId.of("UTC")));
    }

    @Test
    void ageMetWhenProfileAgeIsWithinParsedRange() {
        EligibilityChecklistResponse response = evaluator.evaluate(
                profileBuilder().age(35).build(),
                announcementBuilder().build(),
                eligibilityBuilder().ageMin(19).ageMax(39).build());

        assertThat(item(response, "AGE").status()).isEqualTo(EligibilityCheckStatus.MET);
        assertThat(response.summaryStatus()).isEqualTo(EligibilitySummaryStatus.LIKELY_READY);
    }

    @Test
    void ageNotMetWhenProfileAgeExceedsParsedRange() {
        EligibilityChecklistResponse response = evaluator.evaluate(
                profileBuilder().age(45).build(),
                announcementBuilder().build(),
                eligibilityBuilder().ageMin(19).ageMax(39).build());

        assertThat(item(response, "AGE").status()).isEqualTo(EligibilityCheckStatus.NOT_MET);
        assertThat(response.summaryStatus()).isEqualTo(EligibilitySummaryStatus.HAS_BLOCKERS);
    }

    @Test
    void maritalMetForSingleTargetAndSingleProfile() {
        EligibilityChecklistResponse response = evaluator.evaluate(
                profileBuilder().maritalStatus(MaritalStatus.SINGLE).build(),
                announcementBuilder().build(),
                eligibilityBuilder().maritalTargetType(MaritalTargetType.SINGLE).build());

        assertThat(item(response, "MARITAL").status()).isEqualTo(EligibilityCheckStatus.MET);
    }

    @Test
    void maritalMetForEngagedTargetWhenProfileIsNotMarried() {
        EligibilityChecklistResponse response = evaluator.evaluate(
                profileBuilder().maritalStatus(MaritalStatus.SINGLE).build(),
                announcementBuilder().build(),
                eligibilityBuilder().maritalTargetType(MaritalTargetType.ENGAGED).build());

        assertThat(item(response, "MARITAL").status()).isEqualTo(EligibilityCheckStatus.MET);
    }

    @Test
    void newlywedNeedsVerificationWhenMarriageYearsOrLimitIsMissingForMarriedProfile() {
        EligibilityChecklistResponse response = evaluator.evaluate(
                profileBuilder().maritalStatus(MaritalStatus.MARRIED).marriageYears(null).build(),
                announcementBuilder().build(),
                eligibilityBuilder().maritalTargetType(MaritalTargetType.NEWLYWED).marriageYearLimit(7).build());

        assertThat(item(response, "NEWLYWED").status()).isEqualTo(EligibilityCheckStatus.NEEDS_VERIFICATION);
    }

    @Test
    void newlywedNotMetWhenProfileIsClearlyNotMarried() {
        EligibilityChecklistResponse response = evaluator.evaluate(
                profileBuilder().maritalStatus(MaritalStatus.SINGLE).marriageYears(null).build(),
                announcementBuilder().build(),
                eligibilityBuilder().maritalTargetType(MaritalTargetType.NEWLYWED).marriageYearLimit(null).build());

        assertThat(item(response, "NEWLYWED").status()).isEqualTo(EligibilityCheckStatus.NOT_MET);
    }

    @Test
    void childrenMetWhenProfileCountIsAtLeastMinimum() {
        EligibilityChecklistResponse response = evaluator.evaluate(
                profileBuilder().childrenCount(2).build(),
                announcementBuilder().build(),
                eligibilityBuilder().childrenMinCount(2).build());

        assertThat(item(response, "CHILDREN").status()).isEqualTo(EligibilityCheckStatus.MET);
    }

    @Test
    void homelessNotMetWhenRequiredButProfileIsNotHomeless() {
        EligibilityChecklistResponse response = evaluator.evaluate(
                profileBuilder().homeless(false).build(),
                announcementBuilder().build(),
                eligibilityBuilder().homelessRequired(true).build());

        assertThat(item(response, "HOMELESS").status()).isEqualTo(EligibilityCheckStatus.NOT_MET);
        assertThat(response.summaryStatus()).isEqualTo(EligibilitySummaryStatus.HAS_BLOCKERS);
    }

    @Test
    void lowIncomeMetWhenAnyRelatedProfileFlagIsTrue() {
        EligibilityChecklistResponse response = evaluator.evaluate(
                profileBuilder().lowIncome(false).recipient(false).nearPoverty(true).build(),
                announcementBuilder().build(),
                eligibilityBuilder().lowIncomeRequired(true).build());

        assertThat(item(response, "LOW_INCOME").status()).isEqualTo(EligibilityCheckStatus.MET);
    }

    @Test
    void elderlyMetByProfileFlagOrDefaultAge() {
        EligibilityChecklistResponse byFlag = evaluator.evaluate(
                profileBuilder().age(40).elderly(true).build(),
                announcementBuilder().build(),
                eligibilityBuilder().elderlyRequired(true).elderlyAgeMin(null).build());
        EligibilityChecklistResponse byDefaultAge = evaluator.evaluate(
                profileBuilder().age(65).elderly(false).build(),
                announcementBuilder().build(),
                eligibilityBuilder().elderlyRequired(true).elderlyAgeMin(null).build());

        assertThat(item(byFlag, "ELDERLY").status()).isEqualTo(EligibilityCheckStatus.MET);
        assertThat(item(byDefaultAge, "ELDERLY").status()).isEqualTo(EligibilityCheckStatus.MET);
    }

    @Test
    void budgetItemsCompareAnnouncementAmountsAgainstProfileMaxValues() {
        EligibilityChecklistResponse response = evaluator.evaluate(
                profileBuilder().maxDeposit(5000L).maxMonthlyRent(30L).build(),
                announcementBuilder().depositAmount(5000L).monthlyRentAmount(35L).build(),
                eligibilityBuilder().build());

        assertThat(item(response, "DEPOSIT_BUDGET").status()).isEqualTo(EligibilityCheckStatus.MET);
        assertThat(item(response, "MONTHLY_RENT_BUDGET").status()).isEqualTo(EligibilityCheckStatus.NOT_MET);
    }

    @Test
    void budgetNeedsVerificationWhenUserMaxOrAnnouncementAmountIsMissing() {
        EligibilityChecklistResponse response = evaluator.evaluate(
                profileBuilder().maxDeposit(null).maxMonthlyRent(30L).build(),
                announcementBuilder().depositAmount(5000L).monthlyRentAmount(null).build(),
                eligibilityBuilder().build());

        assertThat(item(response, "DEPOSIT_BUDGET").status()).isEqualTo(EligibilityCheckStatus.NEEDS_VERIFICATION);
        assertThat(item(response, "MONTHLY_RENT_BUDGET").status()).isEqualTo(EligibilityCheckStatus.NEEDS_VERIFICATION);
    }

    @Test
    void applicationPeriodUsesInclusiveDates() {
        EligibilityChecklistResponse response = evaluator.evaluate(
                profileBuilder().build(),
                announcementBuilder()
                        .applicationStartDate(LocalDate.of(2026, 6, 9))
                        .applicationEndDate(LocalDate.of(2026, 6, 9))
                        .build(),
                eligibilityBuilder().build());

        assertThat(item(response, "APPLICATION_PERIOD").status()).isEqualTo(EligibilityCheckStatus.MET);
    }

    @Test
    void applicationPeriodBeforeStartNeedsVerificationAndAfterEndIsNotMet() {
        EligibilityChecklistResponse beforeStart = evaluator.evaluate(
                profileBuilder().build(),
                announcementBuilder()
                        .applicationStartDate(LocalDate.of(2026, 6, 10))
                        .applicationEndDate(LocalDate.of(2026, 6, 20))
                        .build(),
                eligibilityBuilder().build());
        EligibilityChecklistResponse afterEnd = evaluator.evaluate(
                profileBuilder().build(),
                announcementBuilder()
                        .applicationStartDate(LocalDate.of(2026, 6, 1))
                        .applicationEndDate(LocalDate.of(2026, 6, 8))
                        .build(),
                eligibilityBuilder().build());

        assertThat(item(beforeStart, "APPLICATION_PERIOD").status()).isEqualTo(EligibilityCheckStatus.NEEDS_VERIFICATION);
        assertThat(item(afterEnd, "APPLICATION_PERIOD").status()).isEqualTo(EligibilityCheckStatus.NOT_MET);
    }

    @Test
    void applicationPeriodMissingDatesNeedsVerification() {
        EligibilityChecklistResponse response = evaluator.evaluate(
                profileBuilder().build(),
                announcementBuilder().applicationStartDate(null).applicationEndDate(null).build(),
                eligibilityBuilder().build());

        assertThat(item(response, "APPLICATION_PERIOD").status()).isEqualTo(EligibilityCheckStatus.NEEDS_VERIFICATION);
    }

    @Test
    void incomeAssetsNeedsVerificationWithoutExposingRawText() {
        EligibilityChecklistResponse response = evaluator.evaluate(
                profileBuilder().build(),
                announcementBuilder().build(),
                eligibilityBuilder().incomeAssetCriteriaRaw("월평균소득 70% 이하 원문").build());

        EligibilityChecklistItemResponse item = item(response, "INCOME_ASSETS");
        assertThat(item.status()).isEqualTo(EligibilityCheckStatus.NEEDS_VERIFICATION);
        assertThat(item.reason()).doesNotContain("월평균소득 70% 이하 원문");
        assertThat(item.announcementCondition()).doesNotContain("월평균소득 70% 이하 원문");
        assertThat(item.actionLabel()).doesNotContain("월평균소득 70% 이하 원문");
    }

    @Test
    void incomeAssetsNotApplicableWhenRawCriteriaIsBlank() {
        EligibilityChecklistResponse response = evaluator.evaluate(
                profileBuilder().build(),
                announcementBuilder().build(),
                eligibilityBuilder().incomeAssetCriteriaRaw(" ").build());

        assertThat(item(response, "INCOME_ASSETS").status()).isEqualTo(EligibilityCheckStatus.NOT_APPLICABLE);
    }

    @Test
    void summaryStatusIsInsufficientDataWhenEligibilityIsMissing() {
        EligibilityChecklistResponse response = evaluator.evaluate(
                profileBuilder().build(),
                announcementBuilder().build(),
                null);

        assertThat(response.summaryStatus()).isEqualTo(EligibilitySummaryStatus.INSUFFICIENT_DATA);
        assertThat(response.disclaimer()).isEqualTo(EligibilityCheckEvaluator.DISCLAIMER);
        assertThat(response.notApplicableCount()).isGreaterThan(0);
        assertThat(response.items()).extracting(EligibilityChecklistItemResponse::key)
                .containsExactly("AGE", "MARITAL", "NEWLYWED", "CHILDREN", "HOMELESS", "LOW_INCOME", "ELDERLY",
                        "DEPOSIT_BUDGET", "MONTHLY_RENT_BUDGET", "APPLICATION_PERIOD", "INCOME_ASSETS");
    }

    @Test
    void summaryStatusIsLikelyReadyWhenAtLeastOneItemIsMetAndNoVerificationOrBlockers() {
        EligibilityChecklistResponse response = evaluator.evaluate(
                profileBuilder().age(30).maxDeposit(5000L).maxMonthlyRent(30L).build(),
                announcementBuilder().depositAmount(5000L).monthlyRentAmount(30L).build(),
                eligibilityBuilder().ageMin(19).ageMax(39).build());

        assertThat(response.summaryStatus()).isEqualTo(EligibilitySummaryStatus.LIKELY_READY);
        assertThat(response.metCount()).isGreaterThan(0);
        assertThat(response.notMetCount()).isZero();
        assertThat(response.needsVerificationCount()).isZero();
    }

    private EligibilityChecklistItemResponse item(EligibilityChecklistResponse response, String key) {
        return response.items().stream()
                .filter(item -> item.key().equals(key))
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
                .regionLevel2("Suwon")
                .fullAddress("1 Sample-ro Suwon Gyeonggi")
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

    private AnnouncementEligibility.AnnouncementEligibilityBuilder eligibilityBuilder() {
        return AnnouncementEligibility.builder();
    }
}
