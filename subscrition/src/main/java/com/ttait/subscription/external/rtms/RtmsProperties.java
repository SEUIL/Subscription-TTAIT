package com.ttait.subscription.external.rtms;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rtms")
public record RtmsProperties(String serviceKey) {
}
