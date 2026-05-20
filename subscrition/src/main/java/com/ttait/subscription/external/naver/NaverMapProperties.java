package com.ttait.subscription.external.naver;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "naver.maps")
public record NaverMapProperties(String clientId, String clientSecret) {
}
