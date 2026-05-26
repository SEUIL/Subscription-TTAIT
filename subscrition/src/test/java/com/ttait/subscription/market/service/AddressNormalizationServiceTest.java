package com.ttait.subscription.market.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ttait.subscription.announcement.domain.AddressResolutionStatus;
import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.announcement.domain.AnnouncementUnitSource;
import com.ttait.subscription.market.domain.LawdCodeMapping;
import com.ttait.subscription.market.repository.LawdCodeMappingRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddressNormalizationServiceTest {

    @Mock
    private LawdCodeMappingRepository lawdCodeMappingRepository;

    private AddressNormalizationService service;

    @BeforeEach
    void setUp() {
        service = new AddressNormalizationService(lawdCodeMappingRepository);
    }

    @Test
    void normalizeUnitAddressResolvesLegalDongAndLawdCode() {
        AnnouncementUnit unit = unit(" 김포시   마산동 ", null);
        LawdCodeMapping mapping = LawdCodeMapping.builder()
                .regionLevel1("경기도")
                .regionLevel2("김포시")
                .legalDongName("마산동")
                .legalDongCode("4157010900")
                .build();
        given(lawdCodeMappingRepository.findFirstByRegionLevel2AndLegalDongNameAndActiveTrue("김포시", "마산동"))
                .willReturn(Optional.of(mapping));

        service.normalizeUnitAddress(unit);

        assertThat(unit.getNormalizedAddress()).isEqualTo("김포시 마산동");
        assertThat(unit.getLegalDongCode()).isEqualTo("4157010900");
        assertThat(unit.getLawdCd()).isEqualTo("41570");
        assertThat(unit.getAddressStatus()).isEqualTo(AddressResolutionStatus.SUCCESS);
        assertThat(unit.getAddressMessage()).isNull();
        assertThat(unit.getAddressNormalizedAt()).isNotNull();
    }

    @Test
    void normalizeUnitAddressUsesLegalDongAfterRegionLevel2InFullAddress() {
        AnnouncementUnit unit = unit("경기도 김포시 마산동 1", "김포시");
        LawdCodeMapping mapping = LawdCodeMapping.builder()
                .regionLevel1("경기도")
                .regionLevel2("김포시")
                .legalDongName("마산동")
                .legalDongCode("4157010900")
                .build();
        given(lawdCodeMappingRepository.findFirstByRegionLevel2AndLegalDongNameAndActiveTrue("김포시", "마산동"))
                .willReturn(Optional.of(mapping));

        service.normalizeUnitAddress(unit);

        assertThat(unit.getNormalizedAddress()).isEqualTo("경기도 김포시 마산동 1");
        assertThat(unit.getLegalDongCode()).isEqualTo("4157010900");
        assertThat(unit.getLawdCd()).isEqualTo("41570");
        assertThat(unit.getAddressStatus()).isEqualTo(AddressResolutionStatus.SUCCESS);
    }

    @Test
    void normalizeUnitAddressInfersRegionWhenStoredRegionLooksLikeLegalDong() {
        AnnouncementUnit unit = unit("김포시 마산동 1", "마산동");
        LawdCodeMapping mapping = LawdCodeMapping.builder()
                .regionLevel1("경기도")
                .regionLevel2("김포시")
                .legalDongName("마산동")
                .legalDongCode("4157010900")
                .build();
        given(lawdCodeMappingRepository.findFirstByRegionLevel2AndLegalDongNameAndActiveTrue("김포시", "마산동"))
                .willReturn(Optional.of(mapping));

        service.normalizeUnitAddress(unit);

        assertThat(unit.getLawdCd()).isEqualTo("41570");
        assertThat(unit.getAddressStatus()).isEqualTo(AddressResolutionStatus.SUCCESS);
    }

    @Test
    void normalizeUnitAddressStoresNoLawdCodeWhenMappingMissing() {
        AnnouncementUnit unit = unit("김포시 마산동", null);
        given(lawdCodeMappingRepository.findFirstByRegionLevel2AndLegalDongNameAndActiveTrue("김포시", "마산동"))
                .willReturn(Optional.empty());

        service.normalizeUnitAddress(unit);

        assertThat(unit.getNormalizedAddress()).isEqualTo("김포시 마산동");
        assertThat(unit.getLegalDongCode()).isNull();
        assertThat(unit.getLawdCd()).isNull();
        assertThat(unit.getAddressStatus()).isEqualTo(AddressResolutionStatus.NO_LAWD_CODE);
        assertThat(unit.getAddressMessage()).isEqualTo("법정동 코드 매핑 없음");
    }

    @Test
    void normalizeUnitAddressStoresNoAddressForBlankAddress() {
        AnnouncementUnit unit = unit(" ", null);

        service.normalizeUnitAddress(unit);

        assertThat(unit.getNormalizedAddress()).isNull();
        assertThat(unit.getLegalDongCode()).isNull();
        assertThat(unit.getLawdCd()).isNull();
        assertThat(unit.getAddressStatus()).isEqualTo(AddressResolutionStatus.NO_ADDRESS);
        assertThat(unit.getAddressMessage()).isEqualTo("주소 없음");
    }

    private AnnouncementUnit unit(String fullAddress, String regionLevel2) {
        return AnnouncementUnit.builder()
                .unitSource(AnnouncementUnitSource.LH_API)
                .sourceUnitKey("unit-key")
                .unitOrder(1)
                .fullAddress(fullAddress)
                .regionLevel2(regionLevel2)
                .build();
    }
}
