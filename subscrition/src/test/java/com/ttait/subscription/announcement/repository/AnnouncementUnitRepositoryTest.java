package com.ttait.subscription.announcement.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.announcement.domain.AnnouncementUnitSource;
import com.ttait.subscription.announcement.domain.GeocodeStatus;
import com.ttait.subscription.announcement.domain.SourceType;
import com.ttait.subscription.config.JpaConfig;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:announcementunitrepository;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class AnnouncementUnitRepositoryTest {

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private AnnouncementUnitRepository unitRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("공고별 미요청 geocoding unit만 unitOrder 순으로 조회한다")
    void findNotRequestedUnitsByAnnouncementId() {
        Announcement target = announcementRepository.save(announcement("PAN-UNIT-001"));
        Announcement other = announcementRepository.save(announcement("PAN-UNIT-002"));

        AnnouncementUnit deletedOldUnit = unitRepository.save(unit(target, "target-deleted", 0));
        deletedOldUnit.softDelete();
        unitRepository.save(unit(target, "target-order-2", 2));
        unitRepository.save(unit(target, "target-success", 3));
        unitRepository.save(unit(other, "other-not-requested", 1));
        unitRepository.save(unit(target, "target-order-1", 1));
        unitRepository.flush();

        unitRepository.findByAnnouncementIdAndUnitSourceAndSourceUnitKeyAndDeletedFalse(
                target.getId(),
                AnnouncementUnitSource.LH_API,
                "target-success"
        ).orElseThrow().markGeocodeSuccess(
                BigDecimal.valueOf(37.5665),
                BigDecimal.valueOf(126.9780),
                LocalDateTime.of(2026, 5, 20, 10, 0)
        );
        unitRepository.flush();

        List<AnnouncementUnit> result = unitRepository
                .findByAnnouncementIdAndGeocodeStatusAndDeletedFalseOrderByUnitOrderAsc(
                        target.getId(),
                        GeocodeStatus.NOT_REQUESTED
                );

        assertThat(result)
                .extracting(AnnouncementUnit::getSourceUnitKey)
                .containsExactly("target-order-1", "target-order-2");
    }

    @Test
    @DisplayName("공고별 unit 조회 시 sourceType resolver가 쓸 announcement를 함께 초기화한다")
    void findUnitsWithAnnouncementByAnnouncementIdInitializesAnnouncement() {
        Announcement target = announcementRepository.save(announcement("PAN-UNIT-003"));
        unitRepository.save(unit(target, "target-order-1", 1));
        unitRepository.flush();
        entityManager.clear();

        List<AnnouncementUnit> result = unitRepository
                .findWithAnnouncementByAnnouncementIdAndDeletedFalseOrderByUnitOrderAsc(target.getId());

        assertThat(result).hasSize(1);
        assertThat(Hibernate.isInitialized(result.get(0).getAnnouncement())).isTrue();
        assertThat(result.get(0).getAnnouncement().getHouseTypeNormalized()).isEqualTo("아파트");
    }

    private Announcement announcement(String sourceNoticeId) {
        return Announcement.builder()
                .sourcePrimary(SourceType.LH)
                .sourceNoticeId(sourceNoticeId)
                .noticeName("테스트 공고")
                .providerName("LH")
                .sourceNoticeUrl("https://example.com/notice/" + sourceNoticeId)
                .noticeStatus(AnnouncementStatus.OPEN)
                .regionLevel1("경기도")
                .supplyTypeRaw("국민임대")
                .supplyTypeNormalized("국민임대")
                .houseTypeRaw("아파트")
                .houseTypeNormalized("아파트")
                .matchKey("match-key-" + sourceNoticeId)
                .merged(false)
                .collectedAt(LocalDateTime.of(2026, 5, 20, 9, 0))
                .build();
    }

    private AnnouncementUnit unit(Announcement announcement, String sourceUnitKey, int unitOrder) {
        return AnnouncementUnit.builder()
                .announcement(announcement)
                .unitSource(AnnouncementUnitSource.LH_API)
                .sourceUnitKey(sourceUnitKey)
                .unitOrder(unitOrder)
                .complexName("테스트 단지")
                .fullAddress("경기도 성남시 테스트로 " + unitOrder)
                .regionLevel1("경기도")
                .regionLevel2("성남시")
                .supplyTypeRaw("국민임대")
                .supplyTypeNormalized("국민임대")
                .houseTypeRaw("아파트")
                .houseTypeNormalized("아파트")
                .build();
    }
}
