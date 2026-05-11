package com.example.demo.external.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external")
public record ExternalApiProperties(
        Provider lh,
        Provider myhome,
        Provider rtms
) {
    public record Provider(String serviceKey) {
    }
}
