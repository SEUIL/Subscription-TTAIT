package com.ttait.subscription.dev;

import com.ttait.subscription.external.naver.NaverGeocodingClient;
import com.ttait.subscription.external.naver.NaverGeocodingResult;
import com.ttait.subscription.external.naver.NaverMapProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Profile("local")
@Controller
public class NaverGeocodeTestController {

    private final NaverGeocodingClient naverGeocodingClient;
    private final NaverMapProperties naverMapProperties;

    public NaverGeocodeTestController(NaverGeocodingClient naverGeocodingClient, NaverMapProperties naverMapProperties) {
        this.naverGeocodingClient = naverGeocodingClient;
        this.naverMapProperties = naverMapProperties;
    }

    @GetMapping("/dev/naver-geocode")
    public String page() {
        return "naver-geocode-test";
    }

    @GetMapping("/dev/naver-map")
    public String mapPage(Model model) {
        model.addAttribute("naverMapClientId", naverMapProperties.clientId());
        return "naver-map-test";
    }

    @ResponseBody
    @GetMapping("/api/dev/naver-geocode")
    public NaverGeocodingResult geocode(@RequestParam String query) {
        return naverGeocodingClient.geocode(query);
    }
}
