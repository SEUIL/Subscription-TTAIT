package com.ttait.subscription.external.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.announcement.domain.AnnouncementUnitSource;
import com.ttait.subscription.announcement.domain.GeocodeStatus;
import com.ttait.subscription.announcement.repository.AnnouncementUnitRepository;
import com.ttait.subscription.external.naver.NaverGeocodingClient;
import com.ttait.subscription.external.naver.NaverGeocodingResult;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnouncementUnitGeocodingEnrichmentServiceTest {

    @Mock
    private AnnouncementUnitRepository announcementUnitRepository;
    @Mock
    private NaverGeocodingClient naverGeocodingClient;

    private AnnouncementUnitGeocodingEnrichmentService service;

    @BeforeEach
    void setUp() {
        service = new AnnouncementUnitGeocodingEnrichmentService(
                announcementUnitRepository,
                naverGeocodingClient
        );
    }

    @Test
    void enrichNotRequestedUnitsMapsAllOutcomesWithoutBlockingOtherUnits() {
        AnnouncementUnit success = unit("success", "경기도 성남시 성공로 1");
        AnnouncementUnit blankAddress = unit("blank", " ");
        AnnouncementUnit noResult = unit("no-result", "경기도 성남시 결과없음로 2");
        AnnouncementUnit failedResult = unit("failed-result", "경기도 성남시 실패로 3");
        AnnouncementUnit clientFailure = unit("client-failure", "경기도 성남시 예외로 4");
        given(announcementUnitRepository.findByAnnouncementIdAndGeocodeStatusAndDeletedFalseOrderByUnitOrderAsc(
                10L,
                GeocodeStatus.NOT_REQUESTED
        )).willReturn(List.of(success, blankAddress, noResult, failedResult, clientFailure));
        given(naverGeocodingClient.geocode(success.getFullAddress()))
                .willReturn(NaverGeocodingResult.success(new BigDecimal("37.5665000"), new BigDecimal("126.9780000")));
        given(naverGeocodingClient.geocode(noResult.getFullAddress()))
                .willReturn(NaverGeocodingResult.noResult("검색 결과 없음"));
        given(naverGeocodingClient.geocode(failedResult.getFullAddress()))
                .willReturn(NaverGeocodingResult.failed("API 오류"));
        given(naverGeocodingClient.geocode(clientFailure.getFullAddress()))
                .willThrow(new RuntimeException("timeout"));

        service.enrichNotRequestedUnits(10L);

        assertThat(success.getGeocodeStatus()).isEqualTo(GeocodeStatus.SUCCESS);
        assertThat(success.getLatitude()).isEqualByComparingTo(new BigDecimal("37.5665000"));
        assertThat(success.getLongitude()).isEqualByComparingTo(new BigDecimal("126.9780000"));
        assertThat(success.getGeocodeMessage()).isNull();
        assertThat(blankAddress.getGeocodeStatus()).isEqualTo(GeocodeStatus.SKIPPED_NO_ADDRESS);
        assertThat(blankAddress.getGeocodeMessage()).isEqualTo("주소 없음");
        assertThat(noResult.getGeocodeStatus()).isEqualTo(GeocodeStatus.NO_RESULT);
        assertThat(noResult.getGeocodeMessage()).isEqualTo("검색 결과 없음");
        assertThat(failedResult.getGeocodeStatus()).isEqualTo(GeocodeStatus.FAILED);
        assertThat(failedResult.getGeocodeMessage()).isEqualTo("API 오류");
        assertThat(clientFailure.getGeocodeStatus()).isEqualTo(GeocodeStatus.FAILED);
        assertThat(clientFailure.getGeocodeMessage()).isEqualTo("geocoding 실패");
        assertThat(List.of(success, blankAddress, noResult, failedResult, clientFailure))
                .extracting(AnnouncementUnit::getGeocodedAt)
                .doesNotContainNull();
        then(naverGeocodingClient).should(never()).geocode(blankAddress.getFullAddress());
        then(announcementUnitRepository).should().save(success);
        then(announcementUnitRepository).should().save(blankAddress);
        then(announcementUnitRepository).should().save(noResult);
        then(announcementUnitRepository).should().save(failedResult);
        then(announcementUnitRepository).should().save(clientFailure);
    }

    private AnnouncementUnit unit(String sourceUnitKey, String fullAddress) {
        return AnnouncementUnit.builder()
                .unitSource(AnnouncementUnitSource.LH_API)
                .sourceUnitKey(sourceUnitKey)
                .unitOrder(1)
                .complexName("테스트 단지")
                .fullAddress(fullAddress)
                .regionLevel1("경기도")
                .regionLevel2("성남시")
                .build();
    }
}
