package com.ttait.subscription.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttait.subscription.admin.dto.MarketRtmsSnapshotBatchRequest;
import com.ttait.subscription.admin.dto.MarketRtmsSnapshotBatchResponse;
import com.ttait.subscription.admin.dto.MarketSnapshotAggregateResponse;
import com.ttait.subscription.admin.dto.RtmsCollectionAllResponse;
import com.ttait.subscription.admin.service.AdminMarketBatchService;
import com.ttait.subscription.external.rtms.RtmsSourceType;
import com.ttait.subscription.market.domain.MarketSnapshotStatus;
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
class AdminMarketBatchControllerTest {

    @Mock
    private AdminMarketBatchService batchService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminMarketBatchController(batchService)).build();
    }

    @Test
    void collectRtmsAndAggregateSnapshotReturnsBatchSummary() throws Exception {
        given(batchService.collectRtmsAndAggregateSnapshot(any()))
                .willReturn(new MarketRtmsSnapshotBatchResponse(
                        "SUCCESS",
                        new RtmsCollectionAllResponse(
                                "APT_RENT", "28200", "202405", "SUCCESS", 100, 96, 4, 0, 1, 830, null),
                        new MarketSnapshotAggregateResponse(
                                7L,
                                "APT_RENT",
                                "28200",
                                "202405",
                                "202405",
                                new BigDecimal("35.0"),
                                new BigDecimal("85.0"),
                                5,
                                70000L,
                                70000L,
                                30L,
                                30L,
                                null,
                                null,
                                MarketSnapshotStatus.OK,
                                "snapshot-key",
                                LocalDateTime.of(2026, 5, 26, 12, 0)
                        ),
                        true,
                        null
                ));

        mockMvc.perform(post("/api/admin/market/batches/rtms-snapshot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MarketRtmsSnapshotBatchRequest(
                                RtmsSourceType.APT_RENT,
                                "28200",
                                "202405",
                                100,
                                10,
                                "202405",
                                "202405",
                                new BigDecimal("35.0"),
                                new BigDecimal("85.0"),
                                3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.collection.fetchedCount").value(100))
                .andExpect(jsonPath("$.snapshot.sampleCount").value(5))
                .andExpect(jsonPath("$.snapshotAggregated").value(true));

        then(batchService).should().collectRtmsAndAggregateSnapshot(any());
    }
}
