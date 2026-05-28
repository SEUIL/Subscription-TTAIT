package com.ttait.subscription.market.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ttait.subscription.config.JpaConfig;
import com.ttait.subscription.market.domain.MarketPriceSnapshot;
import com.ttait.subscription.market.domain.MarketSnapshotStatus;
import com.ttait.subscription.market.domain.MarketSourceType;
import com.ttait.subscription.market.domain.MarketTransactionRaw;
import com.ttait.subscription.market.domain.MarketTransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:marketrepository;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class MarketRepositoryTest {

    @Autowired
    private MarketTransactionRawRepository rawRepository;

    @Autowired
    private MarketPriceSnapshotRepository snapshotRepository;

    @Test
    void saveAndFindRawTransactionByPayloadHash() {
        MarketTransactionRaw saved = rawRepository.saveAndFlush(raw("hash-1", new BigDecimal("59.84")));

        assertThat(rawRepository.existsByRawPayloadHash("hash-1")).isTrue();
        assertThat(rawRepository.findByRawPayloadHash("hash-1"))
                .hasValueSatisfying(found -> {
                    assertThat(found.getId()).isEqualTo(saved.getId());
                    assertThat(found.getTransactionType()).isEqualTo(MarketTransactionType.RENT);
                    assertThat(found.getDepositAmount()).isEqualTo(70000L);
                    assertThat(found.getMonthlyRentAmount()).isEqualTo(30L);
                    assertThat(found.getTradeAmount()).isNull();
                });
    }

    @Test
    void findRawTransactionsBySourceLawdDealYmAndAreaRange() {
        rawRepository.save(raw("hash-1", new BigDecimal("59.84")));
        rawRepository.save(raw("hash-2", new BigDecimal("84.99")));
        rawRepository.flush();

        assertThat(rawRepository.findBySourceTypeAndLawdCdAndDealYmBetweenAndExclusiveAreaBetween(
                MarketSourceType.APT_RENT,
                "41570",
                "202401",
                "202412",
                new BigDecimal("50.00"),
                new BigDecimal("70.00")
        )).extracting(MarketTransactionRaw::getRawPayloadHash).containsExactly("hash-1");
    }

    @Test
    void saveAndFindSnapshotByKeyAndAreaCoverage() {
        MarketPriceSnapshot snapshot = snapshotRepository.saveAndFlush(snapshot("snapshot-1"));

        assertThat(snapshotRepository.findBySnapshotKey("snapshot-1")).hasValue(snapshot);
        assertThat(snapshotRepository
                .findFirstBySourceTypeAndLawdCdAndDealYmFromAndDealYmToAndAreaMinLessThanEqualAndAreaMaxGreaterThanEqualOrderByAggregatedAtDesc(
                        MarketSourceType.APT_RENT,
                        "41570",
                        "202401",
                        "202412",
                        new BigDecimal("59.84"),
                        new BigDecimal("59.84")
                ))
                .hasValueSatisfying(found -> {
                    assertThat(found.getSampleCount()).isEqualTo(5);
                    assertThat(found.getAvgDepositAmount()).isEqualTo(70000L);
                    assertThat(found.getStatus()).isEqualTo(MarketSnapshotStatus.OK);
                });
    }

    @Test
    void returnsEmptyWhenSnapshotDoesNotCoverArea() {
        snapshotRepository.saveAndFlush(snapshot("snapshot-1"));

        assertThat(snapshotRepository
                .findFirstBySourceTypeAndLawdCdAndDealYmFromAndDealYmToAndAreaMinLessThanEqualAndAreaMaxGreaterThanEqualOrderByAggregatedAtDesc(
                        MarketSourceType.APT_RENT,
                        "41570",
                        "202401",
                        "202412",
                        new BigDecimal("90.00"),
                        new BigDecimal("90.00")
                )).isEmpty();
    }

    private MarketTransactionRaw raw(String hash, BigDecimal area) {
        return MarketTransactionRaw.builder()
                .sourceType(MarketSourceType.APT_RENT)
                .lawdCd("41570")
                .dealYm("202405")
                .legalDongName("마산동")
                .buildingName("테스트아파트")
                .jibun("123-1")
                .roadName("테스트로")
                .buildYear(2020)
                .exclusiveArea(area)
                .floor(10)
                .depositAmount(70000L)
                .monthlyRentAmount(30L)
                .rawPayloadHash(hash)
                .rawPayload("raw-payload")
                .collectedAt(LocalDateTime.of(2026, 5, 21, 10, 0))
                .build();
    }

    private MarketPriceSnapshot snapshot(String key) {
        return MarketPriceSnapshot.builder()
                .sourceType(MarketSourceType.APT_RENT)
                .lawdCd("41570")
                .dealYmFrom("202401")
                .dealYmTo("202412")
                .areaMin(new BigDecimal("50.00"))
                .areaMax(new BigDecimal("70.00"))
                .sampleCount(5)
                .avgDepositAmount(70000L)
                .medianDepositAmount(71000L)
                .avgMonthlyRentAmount(30L)
                .medianMonthlyRentAmount(32L)
                .status(MarketSnapshotStatus.OK)
                .snapshotKey(key)
                .aggregatedAt(LocalDateTime.of(2026, 5, 21, 11, 0))
                .build();
    }
}
