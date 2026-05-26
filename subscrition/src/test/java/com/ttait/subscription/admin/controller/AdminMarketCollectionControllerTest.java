package com.ttait.subscription.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttait.subscription.admin.dto.RtmsCollectionAllRequest;
import com.ttait.subscription.admin.dto.RtmsCollectionAllResponse;
import com.ttait.subscription.admin.dto.RtmsCollectionRequest;
import com.ttait.subscription.admin.dto.RtmsCollectionResponse;
import com.ttait.subscription.admin.service.AdminMarketCollectionService;
import com.ttait.subscription.external.rtms.RtmsSourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminMarketCollectionControllerTest {

    @Mock
    private AdminMarketCollectionService collectionService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminMarketCollectionController(collectionService)).build();
    }

    @Test
    void collectRtmsUsesAdminMarketSubpathAndReturnsSummary() throws Exception {
        given(collectionService.collectRtms(any())).willReturn(new RtmsCollectionResponse(
                "APT_RENT", "41570", "202405", "SUCCESS", 2, 1, 1, 0, null));

        mockMvc.perform(post("/api/admin/market/rtms/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RtmsCollectionRequest(
                                RtmsSourceType.APT_RENT, "41570", "202405", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("APT_RENT"))
                .andExpect(jsonPath("$.lawdCd").value("41570"))
                .andExpect(jsonPath("$.dealYm").value("202405"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.fetchedCount").value(2))
                .andExpect(jsonPath("$.savedCount").value(1))
                .andExpect(jsonPath("$.duplicateCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(0));

        ArgumentCaptor<RtmsCollectionRequest> request = ArgumentCaptor.forClass(RtmsCollectionRequest.class);
        then(collectionService).should().collectRtms(request.capture());
        assertThat(request.getValue().sourceType()).isEqualTo(RtmsSourceType.APT_RENT);
        assertThat(request.getValue().lawdCd()).isEqualTo("41570");
        assertThat(request.getValue().dealYm()).isEqualTo("202405");
    }
    @Test
    void collectAllRtmsReturnsPageSummary() throws Exception {
        given(collectionService.collectAllRtms(any())).willReturn(new RtmsCollectionAllResponse(
                "APT_RENT", "41570", "202405", "SUCCESS", 150, 140, 10, 0, 2, 150, null));

        mockMvc.perform(post("/api/admin/market/rtms/collect-all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RtmsCollectionAllRequest(
                                RtmsSourceType.APT_RENT, "41570", "202405", null, 10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("APT_RENT"))
                .andExpect(jsonPath("$.fetchedCount").value(150))
                .andExpect(jsonPath("$.savedCount").value(140))
                .andExpect(jsonPath("$.collectedPageCount").value(2))
                .andExpect(jsonPath("$.totalCount").value(150));

        ArgumentCaptor<RtmsCollectionAllRequest> request = ArgumentCaptor.forClass(RtmsCollectionAllRequest.class);
        then(collectionService).should().collectAllRtms(request.capture());
        assertThat(request.getValue().sourceType()).isEqualTo(RtmsSourceType.APT_RENT);
        assertThat(request.getValue().maxPages()).isEqualTo(10);
    }

}
