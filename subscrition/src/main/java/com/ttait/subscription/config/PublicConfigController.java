package com.ttait.subscription.config;

import com.ttait.subscription.external.naver.NaverMapProperties;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class PublicConfigController {

    private final NaverMapProperties naverMapProperties;

    public PublicConfigController(NaverMapProperties naverMapProperties) {
        this.naverMapProperties = naverMapProperties;
    }

    @GetMapping("/naver-maps")
    public NaverMapsConfigResponse naverMaps() {
        String clientId = naverMapProperties.clientId();
        return new NaverMapsConfigResponse(StringUtils.hasText(clientId) ? clientId : null);
    }

    public record NaverMapsConfigResponse(String clientId) {
    }
}
