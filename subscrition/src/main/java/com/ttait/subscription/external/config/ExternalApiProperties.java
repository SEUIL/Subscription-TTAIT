package com.ttait.subscription.external.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external")
public record ExternalApiProperties(Provider lh) {

    public record Provider(String serviceKey) {
    }
}
