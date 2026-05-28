package com.ttait.subscription.external.rtms;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ttait.subscription.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RtmsClientTest {

    @Test
    void missingServiceKeyFailsBeforeRequest() {
        RtmsClient client = new RtmsClient(
                RestClient.builder().build(),
                new RtmsProperties(" "),
                new RtmsResponseAdapter()
        );

        assertThatThrownBy(() -> client.fetch(RtmsSourceType.APT_RENT, "41570", "202405", 1, 10))
                .isInstanceOf(ApiException.class)
                .hasMessage("RTMS service key is missing");
    }

    @Test
    void invalidLawdCdFailsBeforeRequest() {
        RtmsClient client = new RtmsClient(
                RestClient.builder().build(),
                new RtmsProperties("service-key-placeholder"),
                new RtmsResponseAdapter()
        );

        assertThatThrownBy(() -> client.fetch(RtmsSourceType.APT_RENT, "4157", "202405", 1, 10))
                .isInstanceOf(ApiException.class)
                .hasMessage("LAWD_CD must be 5 digits");
    }

    @Test
    void invalidDealYmFailsBeforeRequest() {
        RtmsClient client = new RtmsClient(
                RestClient.builder().build(),
                new RtmsProperties("service-key-placeholder"),
                new RtmsResponseAdapter()
        );

        assertThatThrownBy(() -> client.fetch(RtmsSourceType.APT_RENT, "41570", "2024", 1, 10))
                .isInstanceOf(ApiException.class)
                .hasMessage("DEAL_YMD must be YYYYMM");
    }
}
