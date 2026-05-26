package com.ttait.subscription.external.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ttait.subscription.announcement.domain.AddressResolutionStatus;
import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.announcement.domain.AnnouncementUnitSource;
import com.ttait.subscription.announcement.repository.AnnouncementUnitRepository;
import com.ttait.subscription.market.service.AddressNormalizationService;
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
        service = new AnnouncementUnitAddressEnrichmentService(
                announcementUnitRepository,
                addressNormalizationService
        );
    }

    @Test
    void enrichNotRequestedUnitsNormalizesAndSavesUnits() {
        AnnouncementUnit first = unit("unit-1", "경기도 김포시 마산동 1");
        AnnouncementUnit second = unit("unit-2", "인천광역시 부평구 부평동 2");
        given(announcementUnitRepository.findByAnnouncementIdAndAddressStatusAndDeletedFalseOrderByUnitOrderAsc(
                10L,
                AddressResolutionStatus.NOT_REQUESTED
        )).willReturn(List.of(first, second));

        service.enrichNotRequestedUnits(10L);

        then(addressNormalizationService).should().normalizeUnitAddress(first);
        then(addressNormalizationService).should().normalizeUnitAddress(second);
        then(announcementUnitRepository).should().save(first);
        then(announcementUnitRepository).should().save(second);
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
