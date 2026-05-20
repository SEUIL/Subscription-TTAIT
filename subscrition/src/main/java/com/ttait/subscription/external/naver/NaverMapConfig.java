package com.ttait.subscription.external.naver;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NaverMapProperties.class)
public class NaverMapConfig {
}
