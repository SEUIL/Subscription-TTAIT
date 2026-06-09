package com.ttait.subscription.announcement.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ttait.subscription.announcement.dto.RecommendationFactorCountsResponse;
import com.ttait.subscription.announcement.dto.RecommendationFactorResponse;
import com.ttait.subscription.announcement.dto.RecommendationFactorStatus;
import com.ttait.subscription.announcement.dto.RecommendationReportResponse;
import com.ttait.subscription.announcement.dto.RecommendationReportSummaryStatus;
import com.ttait.subscription.announcement.service.RecommendationReportService;
import com.ttait.subscription.announcement.service.RecommendationService;
import com.ttait.subscription.auth.domain.AuthenticatedUser;
import com.ttait.subscription.common.exception.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class RecommendationControllerTest {

    private static final String DISCLAIMER = "이 리포트는 저장된 프로필과 공고 데이터를 기준으로 추천 결과를 설명하는 참고 자료입니다. 최종 신청 가능 여부와 세부 자격은 공고 원문과 신청 사이트에서 반드시 확인해야 합니다.";

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private RecommendationReportService recommendationReportService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new RecommendationController(recommendationService, recommendationReportService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getRecommendationReportReturnsRouteResponseForAuthenticatedUser() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(10L, "user1"),
                null,
                List.of()));
        given(recommendationReportService.getReport(10L, 1L)).willReturn(response());

        mockMvc.perform(get("/api/recommendations/{announcementId}/report", 1L))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(jsonPath("$.announcementId").value(1))
                .andExpect(jsonPath("$.noticeName").value("sample notice"))
                .andExpect(jsonPath("$.matchScore").value(88))
                .andExpect(jsonPath("$.summaryStatus").value("RECOMMENDED_WITH_CHECKS"))
                .andExpect(jsonPath("$.existingMatchReasons[0]").value("희망 지역과 일치"))
                .andExpect(jsonPath("$.factorCounts.strongMatch").value(1))
                .andExpect(jsonPath("$.factorCounts.needsVerification").value(2))
                .andExpect(jsonPath("$.factors[0].key").value("REGION"))
                .andExpect(jsonPath("$.factors[0].status").value("STRONG_MATCH"))
                .andExpect(jsonPath("$.factors[1].actionTarget").value("OFFICIAL_NOTICE"))
                .andExpect(jsonPath("$.factors[2].key").value("DETAILED_ELIGIBILITY"))
                .andExpect(jsonPath("$.eligibilityRaw").doesNotExist())
                .andExpect(jsonPath("$.factors[0].eligibilityRaw").doesNotExist());

        then(recommendationReportService).should().getReport(10L, 1L);
    }

    @Test
    void getRecommendationReportRequiresAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/recommendations/{announcementId}/report", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("authentication required"));

        then(recommendationReportService).shouldHaveNoInteractions();
    }

    private RecommendationReportResponse response() {
        return new RecommendationReportResponse(
                1L,
                "sample notice",
                "LH",
                88,
                RecommendationReportSummaryStatus.RECOMMENDED_WITH_CHECKS,
                "추천되지만 확인할 조건이 있습니다.",
                List.of("희망 지역과 일치"),
                new RecommendationFactorCountsResponse(1, 0, 2, 0, 0),
                List.of(
                        new RecommendationFactorResponse(
                                "REGION",
                                "선호 조건",
                                "지역",
                                RecommendationFactorStatus.STRONG_MATCH,
                                "추천 점수에 긍정적",
                                "선호 지역과 공고 지역이 일치합니다.",
                                "Gyeonggi",
                                "Gyeonggi",
                                null,
                                "NONE"),
                        new RecommendationFactorResponse(
                                "INCOME_ASSET",
                                "신청 전 확인",
                                "소득/자산 기준",
                                RecommendationFactorStatus.NEEDS_VERIFICATION,
                                "직접 확인 필요",
                                "소득/자산 상세 기준은 원문 대조가 필요합니다.",
                                "프로필 입력값 참고",
                                "공고 원문 확인 필요",
                                "공고 원문 확인",
                                "OFFICIAL_NOTICE"),
                        new RecommendationFactorResponse(
                                "DETAILED_ELIGIBILITY",
                                "신청 전 확인",
                                "세부 자격조건",
                                RecommendationFactorStatus.NEEDS_VERIFICATION,
                                "직접 확인 필요",
                                "우선순위, 세대원 조건, 제출서류 등 세부 자격조건은 원문 확인이 필요합니다.",
                                "프로필 입력값 참고",
                                "공고 원문 확인 필요",
                                "공고 원문 확인",
                                "OFFICIAL_NOTICE")),
                "https://example.com/PAN-001",
                DISCLAIMER);
    }
}
