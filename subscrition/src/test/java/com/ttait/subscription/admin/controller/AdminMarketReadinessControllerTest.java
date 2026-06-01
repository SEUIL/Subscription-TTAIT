package com.ttait.subscription.admin.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ttait.subscription.admin.dto.MarketReadinessResponse;
import com.ttait.subscription.admin.service.AdminMarketReadinessService;
import com.ttait.subscription.market.domain.MarketSourceType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminMarketReadinessControllerTest {

    @Mock
    private AdminMarketReadinessService readinessService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminMarketReadinessController(readinessService)).build();
    }

    @Test
    void getReadinessReturnsPerUnitBlockerSummary() throws Exception {
        given(readinessService.getReadiness(1L, MarketSourceType.APT_RENT, "202401", "202406"))
                .willReturn(new MarketReadinessResponse(
                        1L,
                        "APT_RENT",
                        "202401",
                        "202406",
                        true,
                        0,
                        1,
                        List.of(new MarketReadinessResponse.UnitReadiness(
                                20L,
                                1,
                                "테스트아파트",
                                "경기도 김포시 마산동 1",
                                "4157010100",
                                "41570",
                                "SUCCESS",
                                null,
                                LocalDateTime.of(2026, 5, 26, 11, 0),
                                new BigDecimal("59.84"),
                                "APT_RENT",
                                0,
                                false,
                                null,
                                false,
                                "SNAPSHOT_NOT_FOUND"
                        ))));

        mockMvc.perform(get("/api/admin/market/announcements/{announcementId}/readiness", 1L)
                        .param("sourceType", "APT_RENT")
                        .param("dealYmFrom", "202401")
                        .param("dealYmTo", "202406"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.announcementId").value(1))
                .andExpect(jsonPath("$.sourceType").value("APT_RENT"))
                .andExpect(jsonPath("$.rtmsServiceKeyConfigured").value(true))
                .andExpect(jsonPath("$.units[0].unitId").value(20))
                .andExpect(jsonPath("$.units[0].lawdCd").value("41570"))
                .andExpect(jsonPath("$.units[0].blocker").value("SNAPSHOT_NOT_FOUND"));

        then(readinessService).should().getReadiness(1L, MarketSourceType.APT_RENT, "202401", "202406");
    }
}
