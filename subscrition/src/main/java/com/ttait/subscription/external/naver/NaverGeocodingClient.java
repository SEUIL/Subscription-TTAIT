package com.ttait.subscription.external.naver;

import com.fasterxml.jackson.databind.JsonNode;
import com.ttait.subscription.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class NaverGeocodingClient {

    private final RestClient restClient;
    private final NaverMapProperties properties;
    private final NaverGeocodingResponseAdapter responseAdapter = new NaverGeocodingResponseAdapter();

    public NaverGeocodingClient(@Qualifier("naverGeocodingRestClient") RestClient restClient,
                                NaverMapProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public NaverGeocodingResult geocode(String query) {
        validateConfig();
        if (!StringUtils.hasText(query)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Address query is required");
        }

        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("maps.apigw.ntruss.com")
                        .path("/map-geocode/v2/geocode")
                        .queryParam("query", query)
                        .build())
                .header("x-ncp-apigw-api-key-id", properties.clientId())
                .header("x-ncp-apigw-api-key", properties.clientSecret())
                .header(HttpHeaders.ACCEPT, "application/json")
                .retrieve()
                .body(JsonNode.class);

        return responseAdapter.adapt(response);
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.clientId()) || !StringUtils.hasText(properties.clientSecret())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Naver Maps clientId/clientSecret is missing");
        }
    }
}
