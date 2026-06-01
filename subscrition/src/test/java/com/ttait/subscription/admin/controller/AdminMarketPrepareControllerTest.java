package com.ttait.subscription.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttait.subscription.admin.dto.MarketPrepareRequest;
import com.ttait.subscription.admin.dto.MarketPrepareResponse;
import com.ttait.subscription.admin.service.AdminMarketPrepareService;
import com.ttait.subscription.external.rtms.RtmsSourceType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminMarketPrepareControllerTest {

    @Mock
    private AdminMarketPrepareService prepareService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminMarketPrepareController(prepareService)).build();
    }

    @Test
    void prepareReturnsBatchAndUnitSummary() throws Exception {
        given(prepareService.prepare(any(), any()))
                .willReturn(new MarketPrepareResponse(
                        1L,
                        "SUCCESS",
                        null,
                        1,
                        0,
                        List.of(new MarketPrepareResponse.UnitPreparation(
                                20L,
                                "APT_RENT",
                                "41570",
                                new BigDecimal("59.84"),
                                new BigDecimal("59.84"),
                                "QUEUED",
                                null
                        )),
                        List.of()
                ));

        mockMvc.perform(post("/api/admin/market/announcements/{announcementId}/prepare", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MarketPrepareRequest(
                                RtmsSourceType.APT_RENT,
                                "202406",
                                100,
                                10,
                                "202401",
                                "202406",
                                3,
                                false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.announcementId").value(1))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.preparedBatchCount").value(1))
                .andExpect(jsonPath("$.units[0].lawdCd").value("41570"));

        then(prepareService).should().prepare(any(), any());
    }
}
