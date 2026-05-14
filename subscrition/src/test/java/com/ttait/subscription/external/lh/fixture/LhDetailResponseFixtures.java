package com.ttait.subscription.external.lh.fixture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class LhDetailResponseFixtures {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private LhDetailResponseFixtures() {
    }

    public static JsonNode singleDsSbdOneUnit() {
        return read("""
                [
                  {
                    "dsSplScdl": [
                      {
                        "ACP_DTTM": "2026.05.20 10:00 ~ 2026.05.22 17:00",
                        "SBSC_ACP_ST_DT": "20260520",
                        "PPR_ACP_ST_DT": "20260525",
                        "PPR_ACP_CLSG_DT": "20260526",
                        "CTRT_ST_DT": "20260601",
                        "CTRT_ED_DT": "20260603"
                      }
                    ],
                    "dsSbd": [
                      {
                        "LCC_NT_NM": "테스트LH아파트 101동",
                        "SBD_NM": "테스트LH아파트",
                        "LGDN_ADR": "경기도 수원시 권선구 테스트로 1",
                        "LGDN_DTL_ADR": "101동",
                        "HSH_CNT": "120",
                        "HTN_FMLA_DESC": "지역난방",
                        "DDO_AR": "59.84"
                      }
                    ],
                    "dsEtcInfo": [
                      { "ETC_CTS": "보증금 5,000만원 / 월임대료 25만원" }
                    ],
                    "dsCtrtPlc": [
                      { "SIL_OFC_TLNO": "031-000-0000" }
                    ]
                  }
                ]
                """);
    }

    public static JsonNode multiDsSbdThreeUnits() {
        return read("""
                [
                  {
                    "dsSplScdl": [
                      { "ACP_DTTM": "2026.06.01 ~ 2026.06.05", "SBSC_ACP_ST_DT": "20260601" }
                    ],
                    "dsSbd": [
                      {
                        "LCC_NT_NM": "복합LH아파트 A단지",
                        "SBD_NM": "복합LH아파트",
                        "LGDN_ADR": "서울특별시 강남구 테스트로 10",
                        "LGDN_DTL_ADR": "A단지",
                        "HSH_CNT": "30",
                        "DDO_AR": "39.72"
                      },
                      {
                        "LCC_NT_NM": "복합LH아파트 A단지",
                        "SBD_NM": "복합LH아파트",
                        "LGDN_ADR": "서울특별시 강남구 테스트로 10",
                        "LGDN_DTL_ADR": "A단지",
                        "HSH_CNT": "20",
                        "DDO_AR": "49.91"
                      },
                      {
                        "LCC_NT_NM": "복합LH아파트 B단지",
                        "SBD_NM": "복합LH아파트",
                        "LGDN_ADR": "서울특별시 강남구 샘플로 20",
                        "LGDN_DTL_ADR": "B단지",
                        "HSH_CNT": "10",
                        "DDO_AR": "59.98"
                      }
                    ],
                    "dsEtcInfo": [
                      { "ETC_CTS": "면적별 보증금과 월임대료는 주택형별로 상이함" }
                    ],
                    "dsCtrtPlc": []
                  }
                ]
                """);
    }

    public static JsonNode missingDsSbdZeroUnits() {
        return read("""
                [
                  {
                    "dsSplScdl": [
                      { "ACP_DTTM": "상시 접수", "SBSC_ACP_ST_DT": null }
                    ],
                    "dsEtcInfo": [
                      { "ETC_CTS": "개별 주택 공고로 상세 단지 목록 없음" }
                    ],
                    "dsCtrtPlc": []
                  }
                ]
                """);
    }

    public static JsonNode nationwideNoticeOneUnitWithoutAddress() {
        return read("""
                [
                  {
                    "dsSplScdl": [
                      { "ACP_DTTM": "2026.07.01 ~ 2026.07.10", "SBSC_ACP_ST_DT": "20260701" }
                    ],
                    "dsSbd": [
                      {
                        "LCC_NT_NM": null,
                        "SBD_NM": "전국 매입임대주택",
                        "LGDN_ADR": null,
                        "LGDN_DTL_ADR": null,
                        "HSH_CNT": "500",
                        "DDO_AR": null
                      }
                    ],
                    "dsEtcInfo": [
                      { "ETC_CTS": "전국 공고로 세부 주소는 지역별 공급목록 직접 확인 필요" }
                    ],
                    "dsCtrtPlc": []
                  }
                ]
                """);
    }

    public static JsonNode mixedRentalAndSaleValuesTwoUnits() {
        return read("""
                [
                  {
                    "dsSplScdl": [
                      { "ACP_DTTM": "2026.08.01 ~ 2026.08.03", "SBSC_ACP_ST_DT": "20260801" }
                    ],
                    "dsSbd": [
                      {
                        "LCC_NT_NM": "혼합LH주택 임대동",
                        "SBD_NM": "혼합LH주택",
                        "LGDN_ADR": "인천광역시 남동구 계약로 3",
                        "LGDN_DTL_ADR": "임대동",
                        "HSH_CNT": "15",
                        "DDO_AR": "36.44",
                        "UNIT_PRICE_RAW": "보증금 800만원 / 월임대료 18만원"
                      },
                      {
                        "LCC_NT_NM": "혼합LH주택 분양동",
                        "SBD_NM": "혼합LH주택",
                        "LGDN_ADR": "인천광역시 남동구 계약로 3",
                        "LGDN_DTL_ADR": "분양동",
                        "HSH_CNT": "8",
                        "DDO_AR": "74.12",
                        "UNIT_PRICE_RAW": "분양가격 18,000만원 ~ 21,850만원"
                      }
                    ],
                    "dsEtcInfo": [
                      { "ETC_CTS": "임대 조건과 분양 조건이 혼재된 공고" }
                    ],
                    "dsCtrtPlc": []
                  }
                ]
                """);
    }

    public static int dsSbdCount(JsonNode response) {
        JsonNode dsSbd = firstPayload(response).path("dsSbd");
        return dsSbd.isArray() ? dsSbd.size() : 0;
    }

    private static JsonNode firstPayload(JsonNode response) {
        return response.isArray() && !response.isEmpty() ? response.get(0) : OBJECT_MAPPER.createObjectNode();
    }

    private static JsonNode read(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid LH detail fixture JSON", e);
        }
    }
}
