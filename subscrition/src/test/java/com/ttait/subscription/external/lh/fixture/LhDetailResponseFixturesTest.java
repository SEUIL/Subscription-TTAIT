package com.ttait.subscription.external.lh.fixture;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LhDetailResponseFixturesTest {

    @Test
    @DisplayName("singleDsSbdOneUnit fixture has one dsSbd row")
    void singleDsSbdOneUnit_hasOneDsSbdRow() {
        JsonNode response = LhDetailResponseFixtures.singleDsSbdOneUnit();

        assertThat(LhDetailResponseFixtures.dsSbdCount(response)).isEqualTo(1);
        assertThat(response.get(0).path("dsSbd").get(0).path("LGDN_ADR").asText()).contains("수원시");
    }

    @Test
    @DisplayName("multiDsSbdThreeUnits fixture has three dsSbd rows")
    void multiDsSbdThreeUnits_hasThreeDsSbdRows() {
        JsonNode response = LhDetailResponseFixtures.multiDsSbdThreeUnits();

        assertThat(LhDetailResponseFixtures.dsSbdCount(response)).isEqualTo(3);
        assertThat(response.get(0).path("dsSbd")).extracting(row -> row.path("DDO_AR").asText())
                .containsExactly("39.72", "49.91", "59.98");
    }

    @Test
    @DisplayName("missingDsSbdZeroUnits fixture has no dsSbd rows")
    void missingDsSbdZeroUnits_hasNoDsSbdRows() {
        JsonNode response = LhDetailResponseFixtures.missingDsSbdZeroUnits();

        assertThat(LhDetailResponseFixtures.dsSbdCount(response)).isZero();
        assertThat(response.get(0).has("dsSbd")).isFalse();
    }

    @Test
    @DisplayName("nationwideNoticeOneUnitWithoutAddress fixture keeps nationwide row without address")
    void nationwideNoticeOneUnitWithoutAddress_hasOneUnitAndNoAddress() {
        JsonNode response = LhDetailResponseFixtures.nationwideNoticeOneUnitWithoutAddress();
        JsonNode unit = response.get(0).path("dsSbd").get(0);

        assertThat(LhDetailResponseFixtures.dsSbdCount(response)).isEqualTo(1);
        assertThat(unit.path("SBD_NM").asText()).contains("전국");
        assertThat(unit.path("LGDN_ADR").isNull()).isTrue();
    }

    @Test
    @DisplayName("mixedRentalAndSaleValuesTwoUnits fixture has rental and sale raw values")
    void mixedRentalAndSaleValuesTwoUnits_hasRentalAndSaleValues() {
        JsonNode response = LhDetailResponseFixtures.mixedRentalAndSaleValuesTwoUnits();
        JsonNode units = response.get(0).path("dsSbd");

        assertThat(LhDetailResponseFixtures.dsSbdCount(response)).isEqualTo(2);
        assertThat(units.get(0).path("UNIT_PRICE_RAW").asText()).contains("보증금", "월임대료");
        assertThat(units.get(1).path("UNIT_PRICE_RAW").asText()).contains("분양가격");
    }
}
