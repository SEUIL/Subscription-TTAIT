package com.ttait.subscription.external.config;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ExternalApiProperties.class)
public class ExternalApiConfig {

    private static final Duration NAVER_GEOCODING_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration NAVER_GEOCODING_READ_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public RestClient naverGeocodingRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(NAVER_GEOCODING_CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(NAVER_GEOCODING_READ_TIMEOUT);
        return builder.requestFactory(requestFactory).build();
    }
}
