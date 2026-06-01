package com.ttait.subscription.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ttait.subscription.external.naver.NaverMapProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PublicConfigControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new PublicConfigController(new NaverMapProperties("public-client-id", "secret-value"))
        ).build();
    }

    @Test
    void naverMapsReturnsClientIdOnly() throws Exception {
        mockMvc.perform(get("/api/config/naver-maps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("public-client-id"))
                .andExpect(jsonPath("$.clientSecret").doesNotExist())
                .andExpect(content().string(not(containsString("secret-value"))))
                .andExpect(content().string(not(containsString("clientSecret"))));
    }
}
