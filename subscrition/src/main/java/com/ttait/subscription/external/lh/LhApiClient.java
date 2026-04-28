package com.ttait.subscription.external.lh;

import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.external.config.ExternalApiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class LhApiClient {

    private final RestClient restClient;
    private final ExternalApiProperties properties;

    public LhApiClient(RestClient restClient, ExternalApiProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public JsonNode fetchNoticeList(int page, int size) {
        validateKey();
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("http")
                        .host("apis.data.go.kr")
                        .path("/B552555/lhLeaseNoticeInfo1/lhLeaseNoticeInfo1")
                        .queryParam("serviceKey", properties.lh().serviceKey())
                        .queryParam("PG_SZ", size)
                        .queryParam("PAGE", page)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode fetchNoticeDetail(String panId, String ccrCnntSysDsCd, String splInfTpCd) {
        validateKey();
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("http")
                        .host("apis.data.go.kr")
                        .path("/B552555/lhLeaseNoticeDtlInfo1/getLeaseNoticeDtlInfo1")
                        .queryParam("serviceKey", properties.lh().serviceKey())
                        .queryParam("PAN_ID", panId)
                        .queryParam("CCR_CNNT_SYS_DS_CD", ccrCnntSysDsCd)
                        .queryParam("SPL_INF_TP_CD", splInfTpCd)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }

    private void validateKey() {
        if (!StringUtils.hasText(properties.lh().serviceKey())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LH service key is missing");
        }
    }
}
