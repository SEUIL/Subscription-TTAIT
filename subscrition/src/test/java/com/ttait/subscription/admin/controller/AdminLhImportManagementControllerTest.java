package com.ttait.subscription.admin.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttait.subscription.admin.dto.LhCandidateCollectionResponse;
import com.ttait.subscription.admin.dto.LhImportCandidateListResponse;
import com.ttait.subscription.admin.dto.LhImportCandidateResponse;
import com.ttait.subscription.admin.dto.LhImportRunResult;
import com.ttait.subscription.admin.dto.LhSelectedImportRequest;
import com.ttait.subscription.admin.service.AdminLhImportManagementService;
import java.util.List;
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
class AdminLhImportManagementControllerTest {

    @Mock
    private AdminLhImportManagementService importManagementService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminLhImportManagementController(importManagementService)).build();
    }

    @Test
    void collectCandidatesUsesNonConflictingAdminSubpath() throws Exception {
        given(importManagementService.collectCandidates(2, 5)).willReturn(
                new LhCandidateCollectionResponse(1, 1, 0, List.of(candidate())));

        mockMvc.perform(post("/api/admin/import/lh/candidates/collect")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].panId").value("PAN-001"));

        then(importManagementService).should().collectCandidates(2, 5);
    }

    @Test
    void listCandidatesSupportsStatusFilter() throws Exception {
        given(importManagementService.listCandidates(0, 20, "COLLECTED")).willReturn(
                new LhImportCandidateListResponse(List.of(candidate()), 1));

        mockMvc.perform(get("/api/admin/import/lh/candidates")
                        .param("status", "COLLECTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));

        then(importManagementService).should().listCandidates(0, 20, "COLLECTED");
    }

    @Test
    void selectedImportAcceptsCandidateIdsAndOmittedForce() throws Exception {
        given(importManagementService.importSelected(org.mockito.ArgumentMatchers.any())).willReturn(
                new LhImportRunResult(2, 2, 0, 0, 0, 2, 0, 0));

        mockMvc.perform(post("/api/admin/import/lh/selected")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LhSelectedImportRequest(List.of(1L, 2L), null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2));

        ArgumentCaptor<LhSelectedImportRequest> request = ArgumentCaptor.forClass(LhSelectedImportRequest.class);
        then(importManagementService).should().importSelected(request.capture());
        org.assertj.core.api.Assertions.assertThat(request.getValue().candidateIds()).containsExactly(1L, 2L);
        org.assertj.core.api.Assertions.assertThat(request.getValue().forceOrDefault()).isFalse();
    }

    @Test
    void forceReparseUsesAnnouncementSubpath() throws Exception {
        given(importManagementService.forceReparse(99L)).willReturn(
                new LhImportRunResult(1, 1, 0, 0, 0, 1, 1, 0));

        mockMvc.perform(post("/api/admin/import/lh/99/force-reparse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reparsed").value(1));

        then(importManagementService).should().forceReparse(eq(99L));
    }

    private LhImportCandidateResponse candidate() {
        return new LhImportCandidateResponse(1L, "PAN-001", "LH notice", "Seoul", "COLLECTED",
                "https://example.com/notice", "https://example.com/notice.pdf",
                false, false, true, "NEW");
    }
}
