package com.ttait.subscription.announcement.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ttait.subscription.market.domain.MarketSnapshotStatus;
import com.ttait.subscription.market.domain.MarketSourceType;
import com.ttait.subscription.market.dto.MarketComparisonResponse;
import com.ttait.subscription.market.dto.MarketComparisonStatus;
import com.ttait.subscription.market.service.MarketComparisonService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AnnouncementMarketComparisonControllerTest {

    @Mock
    private MarketComparisonService marketComparisonService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AnnouncementMarketComparisonController(marketComparisonService)).build();
    }

    @Test
    void compareUnitMarketPriceReturnsComparisonContract() throws Exception {
        given(marketComparisonService.compare(
                eq(10L), eq(20L), eq(MarketSourceType.APT_RENT), eq("202401"), eq("202406")))
                .willReturn(response());

        mockMvc.perform(get("/api/announcements/10/units/20/market-comparison")
                        .param("sourceType", "APT_RENT")
                        .param("dealYmFrom", "202401")
                        .param("dealYmTo", "202406"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.announcementId").value(10))
                .andExpect(jsonPath("$.unitId").value(20))
                .andExpect(jsonPath("$.status").value("COMPARABLE"))
                .andExpect(jsonPath("$.unitPrice.depositAmount").value(75000))
                .andExpect(jsonPath("$.snapshot.sampleCount").value(5))
                .andExpect(jsonPath("$.depositComparison.differenceAmount").value(5000));
    }

    private MarketComparisonResponse response() {
        return new MarketComparisonResponse(
                10L,
                20L,
                "APT_RENT",
                "41570",
                "202401",
                "202406",
                new BigDecimal("59.84"),
                MarketComparisonStatus.COMPARABLE,
                null,
                new MarketComparisonResponse.UnitPrice(75000L, 35L, null, null),
                new MarketComparisonResponse.SnapshotPrice(
                        30L,
                        5,
                        MarketSnapshotStatus.OK,
                        new BigDecimal("50.00"),
                        new BigDecimal("70.00"),
                        70000L,
                        71000L,
                        30L,
                        31L,
                        null,
                        null,
                        LocalDateTime.of(2026, 5, 26, 12, 0)
                ),
                new MarketComparisonResponse.PriceDifference(75000L, 70000L, 5000L, new BigDecimal("7.14")),
                new MarketComparisonResponse.PriceDifference(35L, 30L, 5L, new BigDecimal("16.67")),
                null
        );
    }
}
