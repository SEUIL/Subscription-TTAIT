package com.ttait.subscription.external.rtms;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RtmsResponseAdapterTest {

    private final RtmsResponseAdapter adapter = new RtmsResponseAdapter();

    @Test
    void adaptsRentXmlToTypedTransactions() {
        String xml = """
                <response>
                  <header>
                    <resultCode>00</resultCode>
                    <resultMsg>NORMAL SERVICE.</resultMsg>
                  </header>
                  <body>
                    <items>
                      <item>
                        <법정동>마산동</법정동>
                        <아파트>테스트아파트</아파트>
                        <지번>123-1</지번>
                        <도로명>테스트로</도로명>
                        <건축년도>2020</건축년도>
                        <전용면적>59.84</전용면적>
                        <층>10</층>
                        <보증금액>70,000</보증금액>
                        <월세금액>30</월세금액>
                      </item>
                    </items>
                  </body>
                </response>
                """;

        RtmsApiResult result = adapter.adapt(xml, RtmsSourceType.APT_RENT, "41570", "202405");

        assertThat(result.status()).isEqualTo(RtmsApiResult.Status.SUCCESS);
        assertThat(result.items()).hasSize(1);
        RtmsTransactionItem item = result.items().get(0);
        assertThat(item.sourceType()).isEqualTo(RtmsSourceType.APT_RENT);
        assertThat(item.lawdCd()).isEqualTo("41570");
        assertThat(item.dealYm()).isEqualTo("202405");
        assertThat(item.legalDongName()).isEqualTo("마산동");
        assertThat(item.buildingName()).isEqualTo("테스트아파트");
        assertThat(item.exclusiveArea()).isEqualByComparingTo(new BigDecimal("59.84"));
        assertThat(item.floor()).isEqualTo(10);
        assertThat(item.depositAmount()).isEqualTo(70000L);
        assertThat(item.monthlyRentAmount()).isEqualTo(30L);
        assertThat(item.tradeAmount()).isNull();
    }

    @Test
    void adaptsTradeXmlToTypedTransactions() {
        String xml = """
                <response>
                  <header><resultCode>00</resultCode></header>
                  <body>
                    <items>
                      <item>
                        <법정동>마산동</법정동>
                        <아파트>테스트아파트</아파트>
                        <전용면적>84.99</전용면적>
                        <층>7</층>
                        <거래금액>100,000</거래금액>
                      </item>
                    </items>
                  </body>
                </response>
                """;

        RtmsApiResult result = adapter.adapt(xml, RtmsSourceType.APT_TRADE, "41570", "202405");

        assertThat(result.status()).isEqualTo(RtmsApiResult.Status.SUCCESS);
        RtmsTransactionItem item = result.items().get(0);
        assertThat(item.depositAmount()).isNull();
        assertThat(item.monthlyRentAmount()).isNull();
        assertThat(item.tradeAmount()).isEqualTo(100000L);
    }

    @Test
    void returnsNoResultWhenItemsAreMissing() {
        String xml = """
                <response>
                  <header><resultCode>00</resultCode></header>
                  <body><items /></body>
                </response>
                """;

        RtmsApiResult result = adapter.adapt(xml, RtmsSourceType.APT_RENT, "41570", "202405");

        assertThat(result.status()).isEqualTo(RtmsApiResult.Status.NO_RESULT);
        assertThat(result.items()).isEmpty();
    }

    @Test
    void returnsFailedForApiErrorCode() {
        String xml = """
                <response>
                  <header>
                    <resultCode>99</resultCode>
                    <resultMsg>LIMITED</resultMsg>
                  </header>
                </response>
                """;

        RtmsApiResult result = adapter.adapt(xml, RtmsSourceType.APT_RENT, "41570", "202405");

        assertThat(result.status()).isEqualTo(RtmsApiResult.Status.FAILED);
        assertThat(result.message()).isEqualTo("LIMITED");
    }

    @Test
    void returnsFailedForMalformedAmount() {
        String xml = """
                <response>
                  <header><resultCode>00</resultCode></header>
                  <body>
                    <items>
                      <item>
                        <법정동>마산동</법정동>
                        <아파트>테스트아파트</아파트>
                        <보증금액>not-a-number</보증금액>
                      </item>
                    </items>
                  </body>
                </response>
                """;

        RtmsApiResult result = adapter.adapt(xml, RtmsSourceType.APT_RENT, "41570", "202405");

        assertThat(result.status()).isEqualTo(RtmsApiResult.Status.FAILED);
        assertThat(result.message()).isEqualTo("RTMS response parse failed");
    }
}
