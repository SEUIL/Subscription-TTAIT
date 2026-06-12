package com.ttait.subscription.notification.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.notification.favorite.domain.UserFavoriteAnnouncement;
import com.ttait.subscription.notification.favorite.dto.FavoriteCalendarEventResponse;
import com.ttait.subscription.notification.favorite.dto.FavoriteScheduleGroupResponse;
import com.ttait.subscription.notification.favorite.dto.FavoriteScheduleResponse;
import com.ttait.subscription.notification.favorite.dto.FavoriteScheduleStatus;
import com.ttait.subscription.notification.favorite.repository.UserFavoriteAnnouncementRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FavoriteScheduleServiceTest {

    @Mock
    private UserFavoriteAnnouncementRepository favoriteRepository;

    private FavoriteScheduleService favoriteScheduleService;

    @BeforeEach
    void setUp() {
        favoriteScheduleService = new FavoriteScheduleService(favoriteRepository);
    }

    @Test
    @DisplayName("즐겨찾기가 없으면 빈 일정 응답을 반환한다")
    void getSchedule_whenNoFavorites_returnsEmptySchedule() {
        given(favoriteRepository.findVisibleByUserIdWithAnnouncement(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
            .willReturn(List.of());

        FavoriteScheduleResponse result = favoriteScheduleService.getSchedule(1L, LocalDate.of(2026, 6, 11));

        assertThat(result.summary().totalCount()).isZero();
        assertThat(result.groups())
            .extracting(FavoriteScheduleGroupResponse::key)
            .containsExactly("DUE_TODAY", "DUE_TOMORROW", "DUE_SOON", "OPEN", "UPCOMING", "DATE_UNKNOWN", "CLOSED");
        assertThat(result.calendarEvents()).isEmpty();
        assertThat(result.disclaimer()).contains("최종 신청 기간");
    }

    @Test
    @DisplayName("상태값과 요약 count를 문서 기준으로 계산한다")
    void getSchedule_groupsByStatusAndBuildsSummary() {
        LocalDate today = LocalDate.of(2026, 6, 11);
        List<UserFavoriteAnnouncement> favorites = List.of(
            favorite(1L, "오늘 마감", today.minusDays(2), today, today.minusDays(10)),
            favorite(2L, "내일 마감", today.minusDays(3), today.plusDays(1), today.minusDays(9)),
            favorite(3L, "7일 이내 마감", today.minusDays(1), today.plusDays(5), today.minusDays(8)),
            favorite(4L, "접수 중", today.minusDays(4), today.plusDays(12), today.minusDays(7)),
            favorite(5L, "접수 예정", today.plusDays(2), today.plusDays(14), today.minusDays(6)),
            favorite(6L, "일정 미정", null, today.plusDays(4), today.minusDays(5)),
            favorite(7L, "마감됨", today.minusDays(9), today.minusDays(1), today.minusDays(4))
        );
        given(favoriteRepository.findVisibleByUserIdWithAnnouncement(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
            .willReturn(favorites);

        FavoriteScheduleResponse result = favoriteScheduleService.getSchedule(1L, today);

        assertThat(result.summary().totalCount()).isEqualTo(7);
        assertThat(result.summary().dueTodayCount()).isEqualTo(1);
        assertThat(result.summary().dueTomorrowCount()).isEqualTo(1);
        assertThat(result.summary().dueSoonCount()).isEqualTo(3);
        assertThat(result.summary().openCount()).isEqualTo(4);
        assertThat(result.summary().upcomingCount()).isEqualTo(1);
        assertThat(result.summary().dateUnknownCount()).isEqualTo(1);
        assertThat(result.summary().closedCount()).isEqualTo(1);

        assertThat(result.groups().get(0).items().get(0).scheduleStatus()).isEqualTo(FavoriteScheduleStatus.DUE_TODAY);
        assertThat(result.groups().get(1).items().get(0).scheduleStatus()).isEqualTo(FavoriteScheduleStatus.DUE_TOMORROW);
        assertThat(result.groups().get(2).items().get(0).scheduleStatus()).isEqualTo(FavoriteScheduleStatus.DUE_SOON);
        assertThat(result.groups().get(3).items().get(0).scheduleStatus()).isEqualTo(FavoriteScheduleStatus.OPEN);
        assertThat(result.groups().get(4).items().get(0).scheduleStatus()).isEqualTo(FavoriteScheduleStatus.UPCOMING);
        assertThat(result.groups().get(5).items().get(0).scheduleStatus()).isEqualTo(FavoriteScheduleStatus.DATE_UNKNOWN);
        assertThat(result.groups().get(6).items().get(0).scheduleStatus()).isEqualTo(FavoriteScheduleStatus.CLOSED);
        assertThat(result.groups().get(4).items().get(0).statusMessage()).contains("접수가 시작됩니다");
        assertThat(result.groups().get(6).items().get(0).dDayLabel()).isEqualTo("마감");
    }

    @Test
    @DisplayName("공고일, 신청 시작일, 신청 마감일, 당첨자 발표일을 캘린더 이벤트로 만든다")
    void getSchedule_createsCalendarEvents() {
        LocalDate today = LocalDate.of(2026, 6, 11);
        UserFavoriteAnnouncement favorite = favorite(
            12L,
            "서울 청년 매입임대주택 입주자 모집",
            today.minusDays(1),
            today.plusDays(3),
            today.minusDays(10)
        );
        Announcement announcement = favorite.getAnnouncement();
        given(announcement.getAnnouncementDate()).willReturn(LocalDate.of(2026, 6, 1));
        given(announcement.getWinnerAnnouncementDate()).willReturn(LocalDate.of(2026, 7, 10));
        given(favoriteRepository.findVisibleByUserIdWithAnnouncement(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
            .willReturn(List.of(favorite));

        FavoriteScheduleResponse result = favoriteScheduleService.getSchedule(1L, today);

        assertThat(result.calendarEvents()).hasSize(4);
        assertThat(result.calendarEvents())
            .extracting(FavoriteCalendarEventResponse::eventType)
            .containsExactly("ANNOUNCEMENT_DATE", "APPLICATION_START", "APPLICATION_END", "WINNER_ANNOUNCEMENT");
        assertThat(result.calendarEvents())
            .extracting(FavoriteCalendarEventResponse::title)
            .contains(
                "공고일: 서울 청년 매입임대주택 입주자 모집",
                "신청 시작: 서울 청년 매입임대주택 입주자 모집",
                "신청 마감: 서울 청년 매입임대주택 입주자 모집",
                "당첨자 발표: 서울 청년 매입임대주택 입주자 모집"
            );
    }

    private UserFavoriteAnnouncement favorite(Long id,
                                              String noticeName,
                                              LocalDate applicationStartDate,
                                              LocalDate applicationEndDate,
                                              LocalDate favoritedDate) {
        Announcement announcement = org.mockito.Mockito.mock(Announcement.class);
        given(announcement.getId()).willReturn(id);
        given(announcement.getNoticeName()).willReturn(noticeName);
        given(announcement.getProviderName()).willReturn("LH");
        given(announcement.getNoticeStatus()).willReturn(AnnouncementStatus.OPEN);
        given(announcement.getApplicationStartDate()).willReturn(applicationStartDate);
        given(announcement.getApplicationEndDate()).willReturn(applicationEndDate);
        given(announcement.getAnnouncementDate()).willReturn(null);
        given(announcement.getWinnerAnnouncementDate()).willReturn(null);

        UserFavoriteAnnouncement favorite = org.mockito.Mockito.mock(UserFavoriteAnnouncement.class);
        given(favorite.getAnnouncement()).willReturn(announcement);
        given(favorite.getCreatedAt()).willReturn(favoritedDate.atStartOfDay());
        return favorite;
    }
}
