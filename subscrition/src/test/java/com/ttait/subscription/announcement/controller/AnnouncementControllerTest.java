package com.ttait.subscription.announcement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.announcement.domain.AnnouncementUnitSource;
import com.ttait.subscription.announcement.domain.GeocodeStatus;
import com.ttait.subscription.announcement.domain.ParseReviewStatus;
import com.ttait.subscription.announcement.domain.SourceType;
import com.ttait.subscription.announcement.repository.AnnouncementCategoryRepository;
import com.ttait.subscription.announcement.repository.AnnouncementDetailRepository;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.announcement.repository.AnnouncementUnitRepository;
import com.ttait.subscription.announcement.service.AnnouncementQueryService;
import com.ttait.subscription.external.naver.NaverGeocodingClient;
import com.ttait.subscription.external.naver.NaverGeocodingResult;
import com.ttait.subscription.external.service.AnnouncementUnitGeocodingEnrichmentService;
import com.ttait.subscription.external.support.AnnouncementNormalizer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AnnouncementControllerTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private AnnouncementDetailRepository announcementDetailRepository;

    @Mock
    private AnnouncementCategoryRepository announcementCategoryRepository;

    @Mock
    private AnnouncementUnitRepository announcementUnitRepository;

    @Mock
    private NaverGeocodingClient naverGeocodingClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AnnouncementQueryService queryService = new AnnouncementQueryService(
                announcementRepository,
                announcementDetailRepository,
                announcementCategoryRepository,
                announcementUnitRepository,
                new AnnouncementUnitGeocodingEnrichmentService(announcementUnitRepository, naverGeocodingClient),
                new AnnouncementNormalizer());
        mockMvc = MockMvcBuilders.standaloneSetup(new AnnouncementController(queryService)).build();
    }

    @Test
    void getAnnouncementDetailReturnsGeocodedCoordinates() throws Exception {
        Announcement announcement = announcement();
        AnnouncementUnit unit = unit(announcement);
        given(announcementRepository.findPublicVisibleById(
                1L,
                List.of(ParseReviewStatus.APPROVED, ParseReviewStatus.CORRECTED)))
                .willReturn(Optional.of(announcement));
        given(announcementDetailRepository.findByAnnouncementIdAndDeletedFalse(1L)).willReturn(Optional.empty());
        given(announcementUnitRepository.findByAnnouncementIdAndGeocodeStatusAndDeletedFalseOrderByUnitOrderAsc(
                1L,
                GeocodeStatus.NOT_REQUESTED))
                .willReturn(List.of(unit));
        given(announcementUnitRepository.findByAnnouncementIdAndDeletedFalseOrderByUnitOrderAsc(1L))
                .willReturn(List.of(unit));
        given(naverGeocodingClient.geocode(unit.getFullAddress()))
                .willReturn(NaverGeocodingResult.success(new BigDecimal("37.5665000"), new BigDecimal("126.9780000")));

        mockMvc.perform(get("/api/announcements/{announcementId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.announcementId").value(1))
                .andExpect(jsonPath("$.units[0].unitId").value(10))
                .andExpect(jsonPath("$.units[0].latitude").value(37.5665))
                .andExpect(jsonPath("$.units[0].longitude").value(126.978))
                .andExpect(jsonPath("$.units[0].geocodeStatus").value("SUCCESS"));
    }

    @Test
    void getAnnouncementsMapsLatestSortToAnnouncementDateDescending() throws Exception {
        given(announcementRepository.searchPublicVisible(any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), Pageable.ofSize(10), 0));

        mockMvc.perform(get("/api/announcements")
                        .param("sort", "latest")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(announcementRepository).should().searchPublicVisible(any(), pageableCaptor.capture());
        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("announcementDate");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    private Announcement announcement() {
        Announcement announcement = Announcement.builder()
                .sourcePrimary(SourceType.LH)
                .sourceNoticeId("PAN-001")
                .noticeName("sample notice")
                .providerName("LH")
                .sourceNoticeUrl("https://example.com/PAN-001")
                .noticeStatus(AnnouncementStatus.OPEN)
                .announcementDate(LocalDate.of(2026, 5, 1))
                .applicationStartDate(LocalDate.of(2026, 6, 1))
                .applicationEndDate(LocalDate.of(2026, 6, 10))
                .winnerAnnouncementDate(LocalDate.of(2026, 7, 1))
                .regionLevel1("Gyeonggi")
                .regionLevel2("Suwon")
                .fullAddress("1 Sample-ro Suwon Gyeonggi")
                .complexName("Sample Complex")
                .supplyTypeNormalized("Public Rental")
                .houseTypeNormalized("Apartment")
                .depositAmount(5000L)
                .monthlyRentAmount(25L)
                .supplyHouseholdCount(30)
                .matchKey("match-key")
                .merged(false)
                .collectedAt(LocalDateTime.of(2026, 5, 1, 0, 0))
                .build();
        ReflectionTestUtils.setField(announcement, "id", 1L);
        return announcement;
    }

    private AnnouncementUnit unit(Announcement announcement) {
        AnnouncementUnit unit = AnnouncementUnit.builder()
                .announcement(announcement)
                .unitSource(AnnouncementUnitSource.MERGED)
                .sourceUnitKey("unit-key-1")
                .unitOrder(1)
                .complexName("Sample Complex")
                .fullAddress("1 Sample-ro Suwon Gyeonggi")
                .regionLevel1("Gyeonggi")
                .regionLevel2("Suwon")
                .supplyTypeNormalized("Public Rental")
                .houseTypeNormalized("Apartment")
                .depositAmount(5000L)
                .monthlyRentAmount(25L)
                .supplyHouseholdCount(30)
                .rawText("raw text")
                .build();
        ReflectionTestUtils.setField(unit, "id", 10L);
        return unit;
    }
}

