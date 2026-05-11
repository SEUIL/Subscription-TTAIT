package com.example.demo.external.myhome;

import com.example.demo.common.exception.ApiException;
import com.example.demo.external.config.ExternalApiProperties;
import com.example.demo.external.myhome.dto.MyHomeNoticeApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class MyHomeApiClient {

    private final RestClient restClient;
    private final ExternalApiProperties properties;

    public MyHomeApiClient(RestClient restClient, ExternalApiProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public MyHomeNoticeApiResponse fetchNoticeList(int page, int size) {
        validateKey(properties.myhome().serviceKey(), "MyHome");
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("apis.data.go.kr")
                        .path("/1613000/HWSPR02/rsdtRcritNtcList")
                        .queryParam("serviceKey", properties.myhome().serviceKey())
                        .queryParam("pageNo", page)
                        .queryParam("numOfRows", size)
                        .queryParam("_type", "json")
                        .build())
                .retrieve()
                .body(MyHomeNoticeApiResponse.class);
    }

    private void validateKey(String key, String provider) {
        if (!StringUtils.hasText(key)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, provider + " service key is missing");
        }
    }
}
