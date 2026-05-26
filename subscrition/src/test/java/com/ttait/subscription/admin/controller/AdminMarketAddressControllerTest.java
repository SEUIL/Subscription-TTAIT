package com.ttait.subscription.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttait.subscription.admin.dto.AddressNormalizationRequest;
import com.ttait.subscription.admin.dto.AddressNormalizationResponse;
import com.ttait.subscription.admin.dto.LawdCodeMappingSeedResponse;
import com.ttait.subscription.admin.dto.LawdCodeMappingUpsertRequest;
import com.ttait.subscription.admin.dto.LawdCodeMappingUpsertResponse;
import com.ttait.subscription.admin.service.AdminMarketAddressService;
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
class AdminMarketAddressControllerTest {

    @Mock
    private AdminMarketAddressService addressService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminMarketAddressController(addressService)).build();
    }

    @Test
    void upsertLawdCodeMappingsReturnsSummary() throws Exception {
        given(addressService.upsertLawdCodeMappings(any()))
                .willReturn(new LawdCodeMappingUpsertResponse(2, 1, 1));

        mockMvc.perform(post("/api/admin/market/address/lawd-code-mappings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mappings": [
                                    {
                                      "regionLevel1": "경기도",
                                      "regionLevel2": "김포시",
                                      "legalDongName": "마산동",
                                      "legalDongCode": "4157010900"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(2))
                .andExpect(jsonPath("$.insertedCount").value(1))
                .andExpect(jsonPath("$.updatedCount").value(1));

        ArgumentCaptor<LawdCodeMappingUpsertRequest> request = ArgumentCaptor.forClass(LawdCodeMappingUpsertRequest.class);
        then(addressService).should().upsertLawdCodeMappings(request.capture());
        assertThat(request.getValue().mappings()).hasSize(1);
    }


    @Test
    void importLawdCodeMappingsReturnsSeedSummary() throws Exception {
        given(addressService.importLawdCodeMappings(any()))
                .willReturn(new LawdCodeMappingSeedResponse(2, 1, 1, 1, 0));

        mockMvc.perform(post("/api/admin/market/address/lawd-code-mappings/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "delimiter": "tab",
                                  "activeOnly": true,
                                  "content": "법정동코드\\t법정동명\\t폐지여부\\n2823710100\\t인천광역시 부평구 부평동\\t존재"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataLineCount").value(2))
                .andExpect(jsonPath("$.parsedCount").value(1))
                .andExpect(jsonPath("$.skippedCount").value(1))
                .andExpect(jsonPath("$.insertedCount").value(1));

        then(addressService).should().importLawdCodeMappings(any());
    }

    @Test
    void normalizeAnnouncementUnitsPassesRetryOption() throws Exception {
        given(addressService.normalizeAnnouncementUnits(eq(1L), eq(true)))
                .willReturn(new AddressNormalizationResponse(1L, 3, 2, 0, 1));

        mockMvc.perform(post("/api/admin/market/address/announcements/1/normalize-units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddressNormalizationRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.announcementId").value(1))
                .andExpect(jsonPath("$.processedCount").value(3))
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.noLawdCodeCount").value(1));

        then(addressService).should().normalizeAnnouncementUnits(1L, true);
    }
}
