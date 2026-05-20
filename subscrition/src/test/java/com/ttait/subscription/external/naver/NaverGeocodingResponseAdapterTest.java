package com.ttait.subscription.external.naver;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class NaverGeocodingResponseAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NaverGeocodingResponseAdapter adapter = new NaverGeocodingResponseAdapter();

    @Test
    void mapsNaverXToLongitudeAndYToLatitude() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "addresses": [
                    {
                      "x": "127.0123456789",
                      "y": "37.123456789"
                    }
                  ]
                }
                """);

        NaverGeocodingResult result = adapter.adapt(response);

        assertThat(result.status()).isEqualTo(NaverGeocodingResult.Status.SUCCESS);
        assertThat(result.longitude()).isEqualByComparingTo(new BigDecimal("127.0123456789"));
        assertThat(result.latitude()).isEqualByComparingTo(new BigDecimal("37.123456789"));
    }

    @Test
    void returnsNoResultWhenAddressesAreMissing() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "status": "OK"
                }
                """);

        NaverGeocodingResult result = adapter.adapt(response);

        assertThat(result.status()).isEqualTo(NaverGeocodingResult.Status.NO_RESULT);
        assertThat(result.longitude()).isNull();
        assertThat(result.latitude()).isNull();
    }

    @Test
    void returnsFailedWhenCoordinatesAreBlank() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "addresses": [
                    {
                      "x": " ",
                      "y": "37.123456789"
                    }
                  ]
                }
                """);

        NaverGeocodingResult result = adapter.adapt(response);

        assertThat(result.status()).isEqualTo(NaverGeocodingResult.Status.FAILED);
        assertThat(result.longitude()).isNull();
        assertThat(result.latitude()).isNull();
    }

    @Test
    void returnsFailedWhenCoordinatesAreMalformed() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "addresses": [
                    {
                      "x": "not-a-number",
                      "y": "37.123456789"
                    }
                  ]
                }
                """);

        NaverGeocodingResult result = adapter.adapt(response);

        assertThat(result.status()).isEqualTo(NaverGeocodingResult.Status.FAILED);
        assertThat(result.longitude()).isNull();
        assertThat(result.latitude()).isNull();
    }
}
