package com.ttait.subscription.external.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;

import com.ttait.subscription.announcement.domain.AddressResolutionStatus;
import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.announcement.domain.AnnouncementUnitSource;
import com.ttait.subscription.announcement.repository.AnnouncementUnitRepository;
import com.ttait.subscription.market.service.AddressNormalizationService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnouncementUnitAddressEnrichmentServiceTest {

    @Mock
    private AnnouncementUnitRepository announcementUnitRepository;
    @Mock
    private AddressNormalizationService addressNormalizationService;

    private AnnouncementUnitAddressEnrichmentService service;

    @BeforeEach
    void setUp() {
        service = new AnnouncementUnitAddressEnrichmentService(announcementUnitRepository, addressNormalizationService);
    }

    @Test
    void enrichUnitsNormalizesSavesAndCountsOutcomes() {
        AnnouncementUnit success = unit("unit-1", "경기도 김포시 마산동 1");
        AnnouncementUnit noLawdCode = unit("unit-2", "인천광역시 부평구 부평동 2");
        given(announcementUnitRepository.findByAnnouncementIdAndAddressStatusInAndDeletedFalseOrderByUnitOrderAsc(
                10L,
                List.of(AddressResolutionStatus.NOT_REQUESTED, AddressResolutionStatus.NO_LAWD_CODE)
        )).willReturn(List.of(success, noLawdCode));
        willAnswer(invocation -> {
            AnnouncementUnit unit = invocation.getArgument(0);
            if (unit == success) {
                unit.markAddressResolved("경기도 김포시 마산동 1", "4157010900", "41570", LocalDateTime.now());
            } else {
                unit.markAddressNoLawdCode("인천광역시 부평구 부평동 2", "법정동 코드 매핑 없음", LocalDateTime.now());
            }
            return null;
        }).given(addressNormalizationService).normalizeUnitAddress(any(AnnouncementUnit.class));

        AnnouncementUnitAddressEnrichmentService.AddressEnrichmentResult result = service.enrichUnits(10L, true);

        assertThat(result.announcementId()).isEqualTo(10L);
        assertThat(result.processedCount()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.noLawdCodeCount()).isEqualTo(1);
        assertThat(result.noAddressCount()).isZero();
        then(addressNormalizationService).should().normalizeUnitAddress(success);
        then(addressNormalizationService).should().normalizeUnitAddress(noLawdCode);
        then(announcementUnitRepository).should().save(success);
        then(announcementUnitRepository).should().save(noLawdCode);
    }

    @Test
    void enrichNotRequestedUnitsUsesNotRequestedOnly() {
        given(announcementUnitRepository.findByAnnouncementIdAndAddressStatusInAndDeletedFalseOrderByUnitOrderAsc(
                10L,
                List.of(AddressResolutionStatus.NOT_REQUESTED)
        )).willReturn(List.of());

        AnnouncementUnitAddressEnrichmentService.AddressEnrichmentResult result = service.enrichNotRequestedUnits(10L);

        assertThat(result.processedCount()).isZero();
    }

    private AnnouncementUnit unit(String sourceUnitKey, String fullAddress) {
        return AnnouncementUnit.builder()
                .unitSource(AnnouncementUnitSource.LH_API)
                .sourceUnitKey(sourceUnitKey)
                .unitOrder(1)
                .complexName("테스트 단지")
                .fullAddress(fullAddress)
                .regionLevel1("경기도")
                .regionLevel2("김포시")
                .build();
    }
}
