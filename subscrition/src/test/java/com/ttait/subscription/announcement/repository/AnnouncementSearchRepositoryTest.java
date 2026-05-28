package com.ttait.subscription.announcement.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementCategory;
import com.ttait.subscription.announcement.domain.AnnouncementEligibility;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.domain.MatchSource;
import com.ttait.subscription.announcement.domain.ParseReviewStatus;
import com.ttait.subscription.announcement.domain.SourceType;
import com.ttait.subscription.config.JpaConfig;
import com.ttait.subscription.user.domain.enums.CategoryCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:announcementsearchrepository;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class AnnouncementSearchRepositoryTest {

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private AnnouncementEligibilityRepository eligibilityRepository;

    @Autowired
    private AnnouncementCategoryRepository categoryRepository;

    @Test
    @DisplayName("public visible 공고를 DB에서 필터링하고 page 단위로 정렬 조회한다")
    void searchPublicVisibleFiltersAndPagesInDatabaseOrder() {
        saveVisible(announcement("PAN-001", "서울 청년 임대", "서울특별시", "강남구",
                "국민임대", "아파트", AnnouncementStatus.OPEN, 1000L, 20L,
                LocalDate.of(2026, 6, 10)));
        Announcement first = saveVisible(announcement("PAN-002", "서울 신혼 임대", "서울특별시", "송파구",
                "국민임대", "아파트", AnnouncementStatus.OPEN, 2000L, 30L,
                LocalDate.of(2026, 6, 1)));
        saveVisible(announcement("PAN-003", "부산 청년 임대", "부산광역시", "해운대구",
                "국민임대", "아파트", AnnouncementStatus.OPEN, 1000L, 20L,
                LocalDate.of(2026, 6, 3)));
        saveRejected(announcement("PAN-004", "서울 청년 거부", "서울특별시", "강남구",
                "국민임대", "아파트", AnnouncementStatus.OPEN, 1000L, 20L,
                LocalDate.of(2026, 6, 2)));

        AnnouncementSearchCondition condition = condition(
                "서울특별시", null, "국민임대", "아파트", null, AnnouncementStatus.OPEN,
                null, 1000L, 2500L, null, 30L, null);

        Page<Announcement> page = announcementRepository.searchPublicVisible(condition, PageRequest.of(0, 1));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Announcement::getId).containsExactly(first.getId());
    }

    @Test
    @DisplayName("category 필터는 저장된 category를 우선하고 저장값이 없을 때만 키워드 fallback을 사용한다")
    void searchPublicVisibleUsesCategoryFallbackOnlyWhenNoStoredCategoryExists() {
        Announcement storedYouth = saveVisible(announcement("PAN-CAT-001", "청년 임대", "서울특별시", "강남구",
                "국민임대", "아파트", AnnouncementStatus.OPEN, 1000L, 20L,
                LocalDate.of(2026, 6, 1)));
        categoryRepository.save(category(storedYouth, CategoryCode.YOUTH));

        saveVisible(announcement("PAN-CAT-002", "청년 문구만 있는 공고", "서울특별시", "강남구",
                "국민임대", "아파트", AnnouncementStatus.OPEN, 1000L, 20L,
                LocalDate.of(2026, 6, 2)));

        Announcement storedOther = saveVisible(announcement("PAN-CAT-003", "청년 문구와 다른 저장 category", "서울특별시", "강남구",
                "국민임대", "아파트", AnnouncementStatus.OPEN, 1000L, 20L,
                LocalDate.of(2026, 6, 3)));
        categoryRepository.save(category(storedOther, CategoryCode.ELDERLY));

        AnnouncementSearchCondition condition = condition(
                null, null, null, null, null, null,
                null, null, null, null, null, List.of(CategoryCode.YOUTH));

        Page<Announcement> page = announcementRepository.searchPublicVisible(condition, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Announcement::getSourceNoticeId)
                .containsExactly("PAN-CAT-001", "PAN-CAT-002");
    }

    @Test
    @DisplayName("regionLevel2 필터는 부분 문자열이 아니라 시군구 token 단위로 매칭한다")
    void searchPublicVisibleMatchesRegionLevel2ByToken() {
        saveVisible(announcement("PAN-REGION-001", "강남구 공고", "서울특별시", "강남구",
                "국민임대", "아파트", AnnouncementStatus.OPEN, 1000L, 20L,
                LocalDate.of(2026, 6, 1)));
        saveVisible(announcement("PAN-REGION-002", "남구 공고", "부산광역시", "남구",
                "국민임대", "아파트", AnnouncementStatus.OPEN, 1000L, 20L,
                LocalDate.of(2026, 6, 2)));

        AnnouncementSearchCondition condition = condition(
                null, "남구", null, null, null, null,
                null, null, null, null, null, null);

        Page<Announcement> page = announcementRepository.searchPublicVisible(condition, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Announcement::getSourceNoticeId)
                .containsExactly("PAN-REGION-002");
    }

    @Test
    @DisplayName("houseType 필터는 raw 값을 정규화한 기존 의미를 유지한다")
    void searchPublicVisibleMatchesNormalizedHouseTypeRawText() {
        saveVisible(announcement("PAN-HOUSE-001", "아파트형 공고", "서울특별시", "강남구",
                "국민임대", "아파트형", "기타", AnnouncementStatus.OPEN, 1000L, 20L,
                LocalDate.of(2026, 6, 1)));

        AnnouncementSearchCondition condition = condition(
                null, null, null, "아파트", null, null,
                null, null, null, null, null, null);

        Page<Announcement> page = announcementRepository.searchPublicVisible(condition, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Announcement::getSourceNoticeId)
                .containsExactly("PAN-HOUSE-001");
    }

    @Test
    @DisplayName("keyword 필터는 SQL wildcard를 literal 문자로 취급한다")
    void searchPublicVisibleEscapesKeywordWildcards() {
        saveVisible(announcement("PAN-KEYWORD-001", "일반 공고", "서울특별시", "강남구",
                "국민임대", "아파트", AnnouncementStatus.OPEN, 1000L, 20L,
                LocalDate.of(2026, 6, 1)));
        saveVisible(announcement("PAN-KEYWORD-002", "할인_특가 공고", "서울특별시", "강남구",
                "국민임대", "아파트", AnnouncementStatus.OPEN, 1000L, 20L,
                LocalDate.of(2026, 6, 2)));

        AnnouncementSearchCondition condition = condition(
                null, null, null, null, null, null,
                "_", null, null, null, null, null);

        Page<Announcement> page = announcementRepository.searchPublicVisible(condition, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Announcement::getSourceNoticeId)
                .containsExactly("PAN-KEYWORD-002");
    }

    private Announcement saveVisible(Announcement announcement) {
        Announcement saved = announcementRepository.save(announcement);
        AnnouncementEligibility eligibility = AnnouncementEligibility.builder()
                .announcement(saved)
                .build();
        eligibility.approve("tester");
        eligibilityRepository.save(eligibility);
        return saved;
    }

    private void saveRejected(Announcement announcement) {
        Announcement saved = announcementRepository.save(announcement);
        eligibilityRepository.save(AnnouncementEligibility.builder()
                .announcement(saved)
                .build());
    }

    private AnnouncementCategory category(Announcement announcement, CategoryCode categoryCode) {
        return AnnouncementCategory.builder()
                .announcement(announcement)
                .categoryCode(categoryCode)
                .matchSource(MatchSource.RULE)
                .matchReason("test")
                .score(100)
                .build();
    }

    private AnnouncementSearchCondition condition(String regionLevel1,
                                                  String regionLevel2,
                                                  String supplyType,
                                                  String houseType,
                                                  String provider,
                                                  AnnouncementStatus status,
                                                  String keyword,
                                                  Long minDeposit,
                                                  Long maxDeposit,
                                                  Long minMonthlyRent,
                                                  Long maxMonthlyRent,
                                                  List<CategoryCode> categories) {
        return new AnnouncementSearchCondition(
                regionLevel1,
                regionLevel2,
                supplyType,
                houseType,
                provider,
                status,
                keyword,
                minDeposit,
                maxDeposit,
                minMonthlyRent,
                maxMonthlyRent,
                categories,
                List.of(ParseReviewStatus.APPROVED, ParseReviewStatus.CORRECTED)
        );
    }

    private Announcement announcement(String sourceNoticeId,
                                      String noticeName,
                                      String regionLevel1,
                                      String regionLevel2,
                                      String supplyType,
                                      String houseType,
                                      AnnouncementStatus status,
                                      Long depositAmount,
                                      Long monthlyRentAmount,
                                      LocalDate applicationEndDate) {
        return announcement(sourceNoticeId, noticeName, regionLevel1, regionLevel2, supplyType, houseType, houseType,
                status, depositAmount, monthlyRentAmount, applicationEndDate);
    }

    private Announcement announcement(String sourceNoticeId,
                                      String noticeName,
                                      String regionLevel1,
                                      String regionLevel2,
                                      String supplyType,
                                      String houseTypeRaw,
                                      String houseTypeNormalized,
                                      AnnouncementStatus status,
                                      Long depositAmount,
                                      Long monthlyRentAmount,
                                      LocalDate applicationEndDate) {
        return Announcement.builder()
                .sourcePrimary(SourceType.LH)
                .sourceNoticeId(sourceNoticeId)
                .noticeName(noticeName)
                .providerName("LH")
                .sourceNoticeUrl("https://example.com/notice/" + sourceNoticeId)
                .noticeStatus(status)
                .regionLevel1(regionLevel1)
                .regionLevel2(regionLevel2)
                .fullAddress(regionLevel1 + " " + regionLevel2 + " 테스트로")
                .complexName("테스트 단지")
                .supplyTypeRaw(supplyType)
                .supplyTypeNormalized(supplyType)
                .houseTypeRaw(houseTypeRaw)
                .houseTypeNormalized(houseTypeNormalized)
                .depositAmount(depositAmount)
                .monthlyRentAmount(monthlyRentAmount)
                .applicationEndDate(applicationEndDate)
                .matchKey("match-key-" + sourceNoticeId)
                .merged(false)
                .collectedAt(LocalDateTime.of(2026, 5, 21, 9, 0))
                .build();
    }
}
