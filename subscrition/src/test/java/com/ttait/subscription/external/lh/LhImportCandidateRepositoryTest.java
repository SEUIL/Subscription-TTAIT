package com.ttait.subscription.external.lh;

import static org.assertj.core.api.Assertions.assertThat;

import com.ttait.subscription.config.JpaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:lhimportcandidate;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class LhImportCandidateRepositoryTest {

    @Autowired
    private LhImportCandidateRepository candidateRepository;

    @Test
    void saveAndFindByPanIdPersistsPlanStagingFields() {
        String listRawJson = """
                {"PAN_ID":"PAN-001"}
                """.trim();
        String detailRawJson = """
                {"dsAhflInfo":[]}
                """.trim();
        LhImportCandidate candidate = new LhImportCandidate("PAN-001");
        candidate.updateCollected(
                "03",
                "050",
                "Test notice",
                "Seoul",
                "OPEN_RAW",
                "https://example.com/notice",
                "https://example.com/notice.pdf",
                false,
                true,
                true,
                "NEW",
                listRawJson,
                detailRawJson,
                "list-hash",
                "detail-hash"
        );

        LhImportCandidate saved = candidateRepository.saveAndFlush(candidate);

        assertThat(candidateRepository.findByPanId(saved.getPanId()))
                .hasValueSatisfying(found -> {
                    assertThat(found.getCcrCnntSysDsCd()).isEqualTo("03");
                    assertThat(found.getSplInfTpCd()).isEqualTo("050");
                    assertThat(found.getTitle()).isEqualTo("Test notice");
                    assertThat(found.getRegionLevel1()).isEqualTo("Seoul");
                    assertThat(found.getRegion()).isEqualTo("Seoul");
                    assertThat(found.getNoticeStatusRaw()).isEqualTo("OPEN_RAW");
                    assertThat(found.getSourceNoticeUrl()).isEqualTo("https://example.com/notice");
                    assertThat(found.getPdfUrl()).isEqualTo("https://example.com/notice.pdf");
                    assertThat(found.isLandNotice()).isFalse();
                    assertThat(found.isCommercialNotice()).isFalse();
                    assertThat(found.isAlreadyImported()).isTrue();
                    assertThat(found.isCanParse()).isTrue();
                    assertThat(found.getSkipReason()).isNull();
                    assertThat(found.getListRawJson()).isEqualTo(listRawJson);
                    assertThat(found.getItemJson()).isEqualTo(listRawJson);
                    assertThat(found.getDetailRawJson()).isEqualTo(detailRawJson);
                    assertThat(found.getDetailJson()).isEqualTo(detailRawJson);
                    assertThat(found.getListHash()).isEqualTo("list-hash");
                    assertThat(found.getDetailHash()).isEqualTo("detail-hash");
                    assertThat(found.getStatus()).isEqualTo(LhImportCandidateStatus.COLLECTED);
                    assertThat(found.getCollectedAt()).isNotNull();
                });
    }
}
