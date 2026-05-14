package com.ttait.subscription.external.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ttait.subscription.external.lh.fixture.LhDetailResponseFixtures;
import com.ttait.subscription.external.support.AnnouncementNormalizer;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LhUnitCandidateExtractorTest {

    private LhUnitCandidateExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new LhUnitCandidateExtractor(new AnnouncementNormalizer());
    }

    @Test
    void singleDsSbdOneUnit_extractsOneCandidate() {
        List<LhUnitCandidate> candidates = extractor.extract(
                "PAN-001",
                LhDetailResponseFixtures.singleDsSbdOneUnit(),
                "국민임대",
                "아파트",
                "경기도");

        assertThat(candidates).hasSize(1);
        LhUnitCandidate candidate = candidates.get(0);
        assertThat(candidate.unitOrder()).isZero();
        assertThat(candidate.complexName()).isEqualTo("테스트LH아파트 101동");
        assertThat(candidate.fullAddress()).contains("수원시");
        assertThat(candidate.regionLevel2()).isEqualTo("수원시");
        assertThat(candidate.exclusiveAreaValue()).isEqualByComparingTo(new BigDecimal("59.84"));
        assertThat(candidate.supplyHouseholdCount()).isEqualTo(120);
    }

    @Test
    void multiDsSbdThreeUnits_extractsThreeCandidatesWithStableKeys() {
        List<LhUnitCandidate> first = extractor.extract(
                "PAN-002",
                LhDetailResponseFixtures.multiDsSbdThreeUnits(),
                "행복주택",
                "아파트",
                "서울특별시");
        List<LhUnitCandidate> second = extractor.extract(
                "PAN-002",
                LhDetailResponseFixtures.multiDsSbdThreeUnits(),
                "행복주택",
                "아파트",
                "서울특별시");

        assertThat(first).hasSize(3);
        assertThat(first).extracting(LhUnitCandidate::unitOrder).containsExactly(0, 1, 2);
        assertThat(first).extracting(LhUnitCandidate::sourceUnitKey)
                .containsExactlyElementsOf(second.stream().map(LhUnitCandidate::sourceUnitKey).toList());
        assertThat(first).extracting(LhUnitCandidate::exclusiveAreaText)
                .containsExactly("39.72", "49.91", "59.98");
    }

    @Test
    void missingDsSbdZeroUnits_returnsEmptyList() {
        List<LhUnitCandidate> candidates = extractor.extract(
                "PAN-003",
                LhDetailResponseFixtures.missingDsSbdZeroUnits(),
                "매입임대",
                null,
                "전국");

        assertThat(candidates).isEmpty();
    }

    @Test
    void nationwideNoticeOneUnitWithoutAddress_keepsUnitWithoutAddress() {
        List<LhUnitCandidate> candidates = extractor.extract(
                "PAN-004",
                LhDetailResponseFixtures.nationwideNoticeOneUnitWithoutAddress(),
                "매입임대",
                null,
                "전국");

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).complexName()).isEqualTo("전국 매입임대주택");
        assertThat(candidates.get(0).fullAddress()).isNull();
        assertThat(candidates.get(0).regionLevel1()).isEqualTo("전국");
    }

    @Test
    void mixedRentalAndSaleValuesTwoUnits_extractsTwoCandidatesAndKeepsRawRows() {
        List<LhUnitCandidate> candidates = extractor.extract(
                "PAN-005",
                LhDetailResponseFixtures.mixedRentalAndSaleValuesTwoUnits(),
                "분양전환",
                "아파트",
                "인천광역시");

        assertThat(candidates).hasSize(2);
        assertThat(candidates).extracting(LhUnitCandidate::rawText)
                .allSatisfy(raw -> assertThat(raw).contains("UNIT_PRICE_RAW"));
        assertThat(candidates).extracting(LhUnitCandidate::supplyHouseholdCount)
                .containsExactly(15, 8);
    }
}
