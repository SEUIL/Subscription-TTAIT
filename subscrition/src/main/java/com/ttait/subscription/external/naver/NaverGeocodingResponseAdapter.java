package com.ttait.subscription.external.naver;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import org.springframework.util.StringUtils;

final class NaverGeocodingResponseAdapter {

    private static final String NO_ADDRESS_MESSAGE = "Naver geocoding response has no address result";
    private static final String BLANK_COORDINATE_MESSAGE = "Naver geocoding response has blank coordinates";
    private static final String MALFORMED_COORDINATE_MESSAGE = "Naver geocoding response has malformed coordinates";

    NaverGeocodingResult adapt(JsonNode response) {
        JsonNode addresses = response == null ? null : response.path("addresses");
        if (addresses == null || !addresses.isArray() || addresses.isEmpty()) {
            return NaverGeocodingResult.noResult(NO_ADDRESS_MESSAGE);
        }

        JsonNode firstAddress = addresses.get(0);
        String longitudeText = firstAddress.path("x").asText();
        String latitudeText = firstAddress.path("y").asText();
        if (!StringUtils.hasText(longitudeText) || !StringUtils.hasText(latitudeText)) {
            return NaverGeocodingResult.failed(BLANK_COORDINATE_MESSAGE);
        }

        try {
            BigDecimal longitude = new BigDecimal(longitudeText.trim());
            BigDecimal latitude = new BigDecimal(latitudeText.trim());
            return NaverGeocodingResult.success(latitude, longitude);
        } catch (NumberFormatException exception) {
            return NaverGeocodingResult.failed(MALFORMED_COORDINATE_MESSAGE);
        }
    }
}
