package com.ttait.subscription.external.rtms;

import com.ttait.subscription.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class RtmsClient {

    private static final String HOST = "apis.data.go.kr";

    private final RestClient restClient;
    private final RtmsProperties properties;
    private final RtmsResponseAdapter responseAdapter;

    public RtmsClient(@Qualifier("rtmsRestClient") RestClient restClient,
                      RtmsProperties properties,
                      RtmsResponseAdapter responseAdapter) {
        this.restClient = restClient;
        this.properties = properties;
        this.responseAdapter = responseAdapter;
    }

    public RtmsApiResult fetch(RtmsSourceType sourceType, String lawdCd, String dealYm, int pageNo, int numOfRows) {
        validateConfig();
        validateRequest(lawdCd, dealYm, pageNo, numOfRows);

        String response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(HOST)
                        .path(sourceType.path())
                        .queryParam("serviceKey", properties.serviceKey())
                        .queryParam("LAWD_CD", lawdCd)
                        .queryParam("DEAL_YMD", dealYm)
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows)
                        .build())
                .retrieve()
                .body(String.class);
        return responseAdapter.adapt(response, sourceType, lawdCd, dealYm);
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.serviceKey())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "RTMS service key is missing");
        }
    }

    private void validateRequest(String lawdCd, String dealYm, int pageNo, int numOfRows) {
        if (lawdCd == null || !lawdCd.matches("\\d{5}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LAWD_CD must be 5 digits");
        }
        if (dealYm == null || !dealYm.matches("\\d{6}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DEAL_YMD must be YYYYMM");
        }
        if (pageNo < 1 || numOfRows < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "pageNo and numOfRows must be positive");
        }
    }
}
