package com.ttait.subscription.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttait.subscription.admin.dto.MarketSnapshotAggregateRequest;
import com.ttait.subscription.market.domain.MarketSnapshotStatus;
import com.ttait.subscription.market.domain.MarketSourceType;
import com.ttait.subscription.market.service.MarketPriceSnapshotAggregationService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminMarketSnapshotControllerTest {

    @Mock
    private MarketPriceSnapshotAggregationService aggregationService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminMarketSnapshotController(aggregationService)).build();
    }

    @Test
    void aggregateReturnsSnapshotSummary() throws Exception {
        given(aggregationService.aggregate(any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new MarketPriceSnapshotAggregationService.AggregationResult(
                        7L,
                        MarketSourceType.APT_RENT,
                        "41570",
                        "202401",
                        "202406",
                        new BigDecimal("50.00"),
                        new BigDecimal("70.00"),
                        3,
                        70000L,
                        70000L,
                        30L,
                        30L,
                        null,
                        null,
                        MarketSnapshotStatus.OK,
                        "snapshot-key",
                        LocalDateTime.of(2026, 5, 26, 12, 0)
                ));

        mockMvc.perform(post("/api/admin/market/snapshots/aggregate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MarketSnapshotAggregateRequest(
                                MarketSourceType.APT_RENT,
                                "41570",
                                "202401",
                                "202406",
                                new BigDecimal("50.00"),
                                new BigDecimal("70.00"),
                                3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshotId").value(7))
                .andExpect(jsonPath("$.sourceType").value("APT_RENT"))
                .andExpect(jsonPath("$.lawdCd").value("41570"))
                .andExpect(jsonPath("$.sampleCount").value(3))
                .andExpect(jsonPath("$.avgDepositAmount").value(70000))
                .andExpect(jsonPath("$.status").value("OK"));
    }
}
