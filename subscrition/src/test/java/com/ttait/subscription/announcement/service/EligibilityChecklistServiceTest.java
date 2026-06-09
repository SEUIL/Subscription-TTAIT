package com.ttait.subscription.announcement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementEligibility;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.domain.ParseReviewStatus;
import com.ttait.subscription.announcement.domain.SourceType;
import com.ttait.subscription.announcement.dto.EligibilityChecklistItemResponse;
import com.ttait.subscription.announcement.dto.EligibilityChecklistResponse;
import com.ttait.subscription.announcement.dto.EligibilitySummaryStatus;
import com.ttait.subscription.announcement.repository.AnnouncementEligibilityRepository;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.user.domain.UserProfile;
import com.ttait.subscription.user.domain.enums.MaritalStatus;
import com.ttait.subscription.user.repository.UserProfileRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
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
class EligibilityChecklistServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private AnnouncementEligibilityRepository announcementEligibilityRepository;

    @Mock
    private EligibilityCheckEvaluator eligibilityCheckEvaluator;

    private EligibilityChecklistService service;

    @BeforeEach
    void setUp() {
        service = new EligibilityChecklistService(
                userProfileRepository,
                announcementRepository,
                announcementEligibilityRepository,
                eligibilityCheckEvaluator);
    }

    @Test
    void getChecklistThrowsBadRequestWhenProfileIsMissing() {
        given(userProfileRepository.findByUserIdAndDeletedFalse(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getChecklist(10L, 1L))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getMessage()).isEqualTo("profile setup required");
                });

        then(announcementRepository).should(never()).findPublicVisibleById(1L, publicVisibleReviewStatuses());
        then(eligibilityCheckEvaluator).shouldHaveNoInteractions();
    }

    @Test
    void getChecklistThrowsNotFoundWhenAnnouncementIsNotPublicVisible() {
        UserProfile profile = profile();
        given(userProfileRepository.findByUserIdAndDeletedFalse(10L)).willReturn(Optional.of(profile));
        given(announcementRepository.findPublicVisibleById(1L, publicVisibleReviewStatuses())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getChecklist(10L, 1L))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getMessage()).isEqualTo("announcement not found");
                });

        then(announcementRepository).should().findPublicVisibleById(1L, publicVisibleReviewStatuses());
        then(announcementEligibilityRepository).shouldHaveNoInteractions();
        then(eligibilityCheckEvaluator).shouldHaveNoInteractions();
    }

    @Test
    void getChecklistUsesPublicVisibilityGuardAndEvaluatorResponse() {
        UserProfile profile = profile();
        Announcement announcement = announcement();
        AnnouncementEligibility eligibility = AnnouncementEligibility.builder()
                .incomeAssetCriteriaRaw("raw income criteria")
                .eligibilityRaw("raw eligibility")
                .specialSupplyRaw("raw special")
                .build();
        EligibilityChecklistResponse expected = response();
        given(userProfileRepository.findByUserIdAndDeletedFalse(10L)).willReturn(Optional.of(profile));
        given(announcementRepository.findPublicVisibleById(1L, publicVisibleReviewStatuses())).willReturn(Optional.of(announcement));
        given(announcementEligibilityRepository.findByAnnouncementId(1L)).willReturn(Optional.of(eligibility));
        given(eligibilityCheckEvaluator.evaluate(profile, announcement, eligibility)).willReturn(expected);

        EligibilityChecklistResponse actual = service.getChecklist(10L, 1L);

        assertThat(actual).isSameAs(expected);
        assertThat(actual.announcementId()).isEqualTo(1L);
        assertThat(actual.summaryStatus()).isEqualTo(EligibilitySummaryStatus.REVIEW_REQUIRED);
        then(userProfileRepository).should().findByUserIdAndDeletedFalse(10L);
        then(announcementRepository).should().findPublicVisibleById(1L, publicVisibleReviewStatuses());
        then(announcementEligibilityRepository).should().findByAnnouncementId(1L);
        then(eligibilityCheckEvaluator).should().evaluate(profile, announcement, eligibility);
    }

    @Test
    void getChecklistAllowsMissingEligibilityAndPassesNullToEvaluator() {
        UserProfile profile = profile();
        Announcement announcement = announcement();
        EligibilityChecklistResponse expected = new EligibilityChecklistResponse(
                1L,
                EligibilitySummaryStatus.INSUFFICIENT_DATA,
                "파싱된 자격 조건 정보가 부족해 판단할 수 없습니다.",
                0,
                0,
                0,
                0,
                List.of(),
                EligibilityCheckEvaluator.DISCLAIMER);
        given(userProfileRepository.findByUserIdAndDeletedFalse(10L)).willReturn(Optional.of(profile));
        given(announcementRepository.findPublicVisibleById(1L, publicVisibleReviewStatuses())).willReturn(Optional.of(announcement));
        given(announcementEligibilityRepository.findByAnnouncementId(1L)).willReturn(Optional.empty());
        given(eligibilityCheckEvaluator.evaluate(profile, announcement, null)).willReturn(expected);

        EligibilityChecklistResponse actual = service.getChecklist(10L, 1L);

        assertThat(actual.summaryStatus()).isEqualTo(EligibilitySummaryStatus.INSUFFICIENT_DATA);
        then(eligibilityCheckEvaluator).should().evaluate(profile, announcement, null);
    }

    @Test
    void responseDtosDoNotExposeRawFieldNames() {
        assertThat(componentNames(EligibilityChecklistResponse.class)).doesNotContain(
                "eligibilityRaw",
                "specialSupplyRaw",
                "incomeAssetCriteriaRaw");
        assertThat(componentNames(EligibilityChecklistItemResponse.class)).doesNotContain(
                "eligibilityRaw",
                "specialSupplyRaw",
                "incomeAssetCriteriaRaw");
    }

    private List<ParseReviewStatus> publicVisibleReviewStatuses() {
        return List.of(ParseReviewStatus.APPROVED, ParseReviewStatus.CORRECTED);
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
                .fullAddress("1 Sample-ro Suwon Gyeonggi")
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

    private EligibilityChecklistResponse response() {
        return new EligibilityChecklistResponse(
                1L,
                EligibilitySummaryStatus.REVIEW_REQUIRED,
                "대체로 확인 가능하지만 공고 원문 또는 프로필 보완 확인이 필요합니다.",
                0,
                0,
                1,
                0,
                List.of(new EligibilityChecklistItemResponse(
                        "INCOME_ASSETS",
                        "소득·자산",
                        "소득·자산 상세 기준",
                        com.ttait.subscription.announcement.dto.EligibilityCheckStatus.NEEDS_VERIFICATION,
                        "WARNING",
                        "소득·자산 상세 기준은 원문 대조가 필요합니다.",
                        "프로필 소득/자산 정보 참고",
                        "공고 원문 확인 필요",
                        "공고 원문 확인",
                        "OFFICIAL_NOTICE")),
                EligibilityCheckEvaluator.DISCLAIMER);
    }

    private String[] componentNames(Class<? extends Record> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
                .map(component -> component.getName())
                .toArray(String[]::new);
    }
}
