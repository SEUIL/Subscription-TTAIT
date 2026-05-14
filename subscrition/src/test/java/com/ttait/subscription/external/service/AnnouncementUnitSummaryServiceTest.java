package com.ttait.subscription.external.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.announcement.domain.AnnouncementUnitSource;
import com.ttait.subscription.announcement.domain.SourceType;
import com.ttait.subscription.announcement.repository.AnnouncementUnitRepository;
import com.ttait.subscription.external.support.AnnouncementNormalizer;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AnnouncementUnitSummaryServiceTest {

    @Mock
    private AnnouncementUnitRepository announcementUnitRepository;

    private AnnouncementUnitSummaryService service;

    @BeforeEach
    void setUp() {
        service = new AnnouncementUnitSummaryService(announcementUnitRepository, new AnnouncementNormalizer());
    }

    @Test
    void applySummary_derivesRepresentativeFieldsFromUnits() {
        Announcement announcement = announcement();
        given(announcementUnitRepository.findByAnnouncementIdAndDeletedFalseOrderByUnitOrderAsc(1L))
                .willReturn(List.of(
                        unit(0, "경기도 수원시 권선구 테스트로 1", "A단지", "아파트", 5000L, 25L, 30),
                        unit(1, "경기도 수원시 권선구 테스트로 1", "A단지", "아파트", 3000L, 30L, 20)
                ));

        service.applySummary(announcement);

        assertThat(announcement.getFullAddress()).isEqualTo("경기도 수원시 권선구 테스트로 1");
        assertThat(announcement.getComplexName()).isEqualTo("A단지");
        assertThat(announcement.getHouseTypeRaw()).isEqualTo("아파트");
        assertThat(announcement.getHouseTypeNormalized()).isEqualTo("아파트");
        assertThat(announcement.getDepositAmount()).isEqualTo(3000L);
        assertThat(announcement.getMonthlyRentAmount()).isEqualTo(25L);
        assertThat(announcement.getSupplyHouseholdCount()).isEqualTo(50);
    }

    private Announcement announcement() {
        Announcement announcement = Announcement.builder()
                .sourcePrimary(SourceType.LH)
                .sourceNoticeId("PAN-001")
                .noticeName("테스트 공고")
                .providerName("LH")
                .sourceNoticeUrl("https://example.com/notice")
                .noticeStatus(AnnouncementStatus.OPEN)
                .regionLevel1("경기도")
                .supplyTypeRaw("국민임대")
                .supplyTypeNormalized("국민임대")
                .matchKey("match-key")
                .merged(false)
                .collectedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(announcement, "id", 1L);
        return announcement;
    }

    private AnnouncementUnit unit(int order, String address, String complexName, String houseType,
                                  Long deposit, Long rent, Integer count) {
        return AnnouncementUnit.builder()
                .announcement(announcement())
                .unitSource(AnnouncementUnitSource.LH_API)
                .sourceUnitKey("unit-" + order)
                .unitOrder(order)
                .fullAddress(address)
                .complexName(complexName)
                .houseTypeRaw(houseType)
                .houseTypeNormalized(houseType)
                .depositAmount(deposit)
                .monthlyRentAmount(rent)
                .supplyHouseholdCount(count)
                .build();
    }
}
