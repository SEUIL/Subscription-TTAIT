package com.ttait.subscription.external.ai.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PdfParseResultTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void legacyJsonWithoutUnits_deserializesWithNullUnits() throws Exception {
        String json = """
                {
                  "noticeType": "임대",
                  "depositAmountManwon": 5000,
                  "monthlyRentAmountManwon": 25,
                  "houseType": "국민임대주택",
                  "address": "경기도 수원시 권선구 테스트로 1"
                }
                """;

        PdfParseResult result = objectMapper.readValue(json, PdfParseResult.class);

        assertThat(result.noticeType()).isEqualTo("임대");
        assertThat(result.depositAmountManwon()).isEqualTo(5000L);
        assertThat(result.units()).isNull();
        assertThat(result.houseType()).isEqualTo("국민임대주택");
    }

    @Test
    void jsonWithUnits_deserializesUnitArray() throws Exception {
        String json = """
                {
                  "noticeType": "분양전환",
                  "units": [
                    {
                      "complexName": "효천LH천년나무2단지",
                      "address": "광주광역시 남구 효천로 1",
                      "regionLevel1": "광주광역시",
                      "regionLevel2": "남구",
                      "supplyType": "분양전환",
                      "houseType": "아파트",
                      "exclusiveAreaText": "59.84㎡",
                      "exclusiveAreaValue": 59.84,
                      "salePriceMinManwon": 18000,
                      "salePriceMaxManwon": 21850,
                      "salePriceRaw": "분양가격 1억8000만원~2억1850만원",
                      "supplyHouseholdCount": 10,
                      "rawText": "59.84㎡ 10호 분양가격 1억8000만원~2억1850만원",
                      "confidence": 0.91,
                      "sourcePage": 5
                    }
                  ]
                }
                """;

        PdfParseResult result = objectMapper.readValue(json, PdfParseResult.class);

        assertThat(result.units()).hasSize(1);
        PdfParseResult.UnitItem unit = result.units().get(0);
        assertThat(unit.complexName()).isEqualTo("효천LH천년나무2단지");
        assertThat(unit.exclusiveAreaValue()).isEqualTo(59.84);
        assertThat(unit.salePriceMinManwon()).isEqualTo(18000L);
        assertThat(unit.salePriceMaxManwon()).isEqualTo(21850L);
        assertThat(unit.confidence()).isEqualTo(0.91);
    }
}
