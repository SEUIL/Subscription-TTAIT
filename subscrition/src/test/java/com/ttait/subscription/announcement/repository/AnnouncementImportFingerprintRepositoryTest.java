package com.ttait.subscription.announcement.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementImportFingerprint;
import com.ttait.subscription.announcement.domain.AnnouncementImportParseStatus;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.domain.SourceType;
import com.ttait.subscription.config.JpaConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fingerprintrepository;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class AnnouncementImportFingerprintRepositoryTest {

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private AnnouncementImportFingerprintRepository fingerprintRepository;

    @Test
    @DisplayName("공고별 LH import fingerprint 메타데이터를 저장하고 조회한다")
    void saveAndFindByAnnouncementId() {
        Announcement announcement = announcementRepository.save(announcement("PAN-001"));
        LocalDateTime lastCheckedAt = LocalDateTime.of(2026, 5, 13, 10, 0);
        LocalDateTime lastParsedAt = LocalDateTime.of(2026, 5, 13, 10, 5);

        AnnouncementImportFingerprint saved = fingerprintRepository.saveAndFlush(
                AnnouncementImportFingerprint.builder()
                        .announcement(announcement)
                        .lhItemHash("lh-item-hash")
                        .lhDetailHash("lh-detail-hash")
                        .pdfUrl("https://example.com/notice.pdf")
                        .pdfContentHash("pdf-content-hash")
                        .pdfAiJsonHash("pdf-ai-json-hash")
                        .parseStatus(AnnouncementImportParseStatus.PARSED)
                        .lastCheckedAt(lastCheckedAt)
                        .lastParsedAt(lastParsedAt)
                        .lastErrorMessage("previous transient error")
                        .build()
        );

        assertThat(saved.getId()).isNotNull();
        assertThat(fingerprintRepository.existsByAnnouncementId(announcement.getId())).isTrue();
        assertThat(fingerprintRepository.findByAnnouncementId(announcement.getId()))
                .hasValueSatisfying(fingerprint -> {
                    assertThat(fingerprint.getAnnouncement().getId()).isEqualTo(announcement.getId());
                    assertThat(fingerprint.getLhItemHash()).isEqualTo("lh-item-hash");
                    assertThat(fingerprint.getLhDetailHash()).isEqualTo("lh-detail-hash");
                    assertThat(fingerprint.getPdfUrl()).isEqualTo("https://example.com/notice.pdf");
                    assertThat(fingerprint.getPdfContentHash()).isEqualTo("pdf-content-hash");
                    assertThat(fingerprint.getPdfAiJsonHash()).isEqualTo("pdf-ai-json-hash");
                    assertThat(fingerprint.getParseStatus()).isEqualTo(AnnouncementImportParseStatus.PARSED);
                    assertThat(fingerprint.getLastCheckedAt()).isEqualTo(lastCheckedAt);
                    assertThat(fingerprint.getLastParsedAt()).isEqualTo(lastParsedAt);
                    assertThat(fingerprint.getLastErrorMessage()).isEqualTo("previous transient error");
                });
    }

    @Test
    @DisplayName("parseStatus를 지정하지 않으면 PENDING으로 저장한다")
    void saveWithoutParseStatusDefaultsPending() {
        Announcement announcement = announcementRepository.save(announcement("PAN-002"));

        AnnouncementImportFingerprint saved = fingerprintRepository.saveAndFlush(
                AnnouncementImportFingerprint.builder()
                        .announcement(announcement)
                        .lhItemHash("lh-item-hash-2")
                        .build()
        );

        assertThat(saved.getParseStatus()).isEqualTo(AnnouncementImportParseStatus.PENDING);
        assertThat(fingerprintRepository.findByAnnouncementId(announcement.getId()))
                .hasValueSatisfying(fingerprint -> assertThat(fingerprint.getParseStatus())
                        .isEqualTo(AnnouncementImportParseStatus.PENDING));
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
                .collectedAt(LocalDateTime.of(2026, 5, 13, 9, 0))
                .build();
    }
}