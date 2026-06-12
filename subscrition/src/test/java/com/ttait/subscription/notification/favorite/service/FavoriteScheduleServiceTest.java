package com.ttait.subscription.notification.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.domain.SourceType;
import com.ttait.subscription.notification.favorite.domain.UserFavoriteAnnouncement;
import com.ttait.subscription.notification.favorite.dto.FavoriteCalendarEventResponse;
import com.ttait.subscription.notification.favorite.dto.FavoriteScheduleGroupResponse;
import com.ttait.subscription.notification.favorite.dto.FavoriteScheduleResponse;
import com.ttait.subscription.notification.favorite.dto.FavoriteScheduleStatus;
import com.ttait.subscription.notification.favorite.repository.UserFavoriteAnnouncementRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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
        LocalDate today = LocalDate.of(2026, 6, 11);
        given(favoriteRepository.findSchedulePageByUserIdWithAnnouncement(
            eq(1L), any(), eq(today), eq(today.plusDays(1)), eq(today.plusDays(7)), any()))
            .willReturn(schedulePage(List.of(), 0));

        FavoriteScheduleResponse result = favoriteScheduleService.getSchedule(1L, today);

        assertThat(result.summary().totalCount()).isZero();
        assertThat(result.summary().returnedCount()).isZero();
        assertThat(result.summary().truncated()).isFalse();
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
        given(favoriteRepository.findSchedulePageByUserIdWithAnnouncement(
            eq(1L), any(), eq(today), eq(today.plusDays(1)), eq(today.plusDays(7)), any()))
            .willReturn(schedulePage(favorites, 7));

        FavoriteScheduleResponse result = favoriteScheduleService.getSchedule(1L, today);

        assertThat(result.summary().totalCount()).isEqualTo(7);
        assertThat(result.summary().returnedCount()).isEqualTo(7);
        assertThat(result.summary().truncated()).isFalse();
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
            today.minusDays(10),
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 7, 10)
        );
        given(favoriteRepository.findSchedulePageByUserIdWithAnnouncement(
            eq(1L), any(), eq(today), eq(today.plusDays(1)), eq(today.plusDays(7)), any()))
            .willReturn(schedulePage(List.of(favorite), 1));

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

    @Test
    @DisplayName("기준일은 Asia/Seoul Clock으로 계산한다")
    void getSchedule_usesAsiaSeoulClockForToday() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-10T15:30:00Z"), ZoneId.of("Asia/Seoul"));
        favoriteScheduleService = new FavoriteScheduleService(favoriteRepository, clock);
        UserFavoriteAnnouncement favorite = favorite(
            21L,
            "서울 기준 오늘 마감",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 11),
            LocalDate.of(2026, 6, 1)
        );
        LocalDate today = LocalDate.of(2026, 6, 11);
        given(favoriteRepository.findSchedulePageByUserIdWithAnnouncement(
            eq(1L), any(), eq(today), eq(today.plusDays(1)), eq(today.plusDays(7)), any()))
            .willReturn(schedulePage(List.of(favorite), 1));

        FavoriteScheduleResponse result = favoriteScheduleService.getSchedule(1L);

        assertThat(result.summary().dueTodayCount()).isEqualTo(1);
        assertThat(result.groups().get(0).items().get(0).scheduleStatus()).isEqualTo(FavoriteScheduleStatus.DUE_TODAY);
        assertThat(result.groups().get(0).items().get(0).dDayLabel()).isEqualTo("D-day");
    }

    @Test
    @DisplayName("표시 필드가 null 또는 blank여도 fallback 값으로 일정 응답을 만든다")
    void getSchedule_whenDisplayFieldsAreNullOrBlank_usesFallbacks() {
        LocalDate today = LocalDate.of(2026, 6, 11);
        UserFavoriteAnnouncement favorite = favorite(
            31L,
            " ",
            null,
            null,
            today.minusDays(1),
            today.plusDays(2),
            today.minusDays(4),
            today.minusDays(3),
            null
        );
        given(favoriteRepository.findSchedulePageByUserIdWithAnnouncement(
            eq(1L), any(), eq(today), eq(today.plusDays(1)), eq(today.plusDays(7)), any()))
            .willReturn(schedulePage(List.of(favorite), 1));

        FavoriteScheduleResponse result = favoriteScheduleService.getSchedule(1L, today);

        assertThat(result.groups().get(2).items().get(0).noticeName()).isEqualTo("공고명 미확인");
        assertThat(result.groups().get(2).items().get(0).providerName()).isEqualTo("기관 미확인");
        assertThat(result.groups().get(2).items().get(0).noticeStatus()).isEqualTo("UNKNOWN");
        assertThat(result.calendarEvents().get(0).title()).isEqualTo("공고일: 공고명 미확인");
    }

    @Test
    @DisplayName("즐겨찾기 일정 응답은 최대 200개로 제한하고 summary에 절단 여부를 표시한다")
    void getSchedule_whenFavoritesExceedLimit_capsItemsAndExposesSummary() {
        LocalDate today = LocalDate.of(2026, 6, 11);
        List<UserFavoriteAnnouncement> favorites = new ArrayList<>();
        for (long id = 1; id <= 200; id++) {
            favorites.add(favorite(id, "오늘 마감 " + id, today.minusDays(1), today, today.minusDays(2)));
        }
        given(favoriteRepository.findSchedulePageByUserIdWithAnnouncement(
            eq(1L), any(), eq(today), eq(today.plusDays(1)), eq(today.plusDays(7)), any()))
            .willReturn(schedulePage(favorites, 201));

        FavoriteScheduleResponse result = favoriteScheduleService.getSchedule(1L, today);

        assertThat(result.summary().totalCount()).isEqualTo(201);
        assertThat(result.summary().returnedCount()).isEqualTo(200);
        assertThat(result.summary().truncated()).isTrue();
        assertThat(result.summary().dueTodayCount()).isEqualTo(200);
        assertThat(result.groups().stream().mapToInt(group -> group.items().size()).sum()).isEqualTo(200);
    }

    private PageImpl<UserFavoriteAnnouncement> schedulePage(List<UserFavoriteAnnouncement> favorites, long total) {
        return new PageImpl<>(favorites, PageRequest.of(0, 200), total);
    }

    private UserFavoriteAnnouncement favorite(Long id,
                                              String noticeName,
                                              LocalDate applicationStartDate,
                                              LocalDate applicationEndDate,
                                              LocalDate favoritedDate) {
        return favorite(id, noticeName, "LH", AnnouncementStatus.OPEN, applicationStartDate, applicationEndDate,
            favoritedDate, null, null);
    }

    private UserFavoriteAnnouncement favorite(Long id,
                                              String noticeName,
                                              LocalDate applicationStartDate,
                                              LocalDate applicationEndDate,
                                              LocalDate favoritedDate,
                                              LocalDate announcementDate,
                                              LocalDate winnerAnnouncementDate) {
        return favorite(id, noticeName, "LH", AnnouncementStatus.OPEN, applicationStartDate, applicationEndDate,
            favoritedDate, announcementDate, winnerAnnouncementDate);
    }

    private UserFavoriteAnnouncement favorite(Long id,
                                              String noticeName,
                                              String providerName,
                                              AnnouncementStatus noticeStatus,
                                              LocalDate applicationStartDate,
                                              LocalDate applicationEndDate,
                                              LocalDate favoritedDate,
                                              LocalDate announcementDate,
                                              LocalDate winnerAnnouncementDate) {
        Announcement announcement = new Announcement(
            SourceType.LH,
            "notice-" + id,
            noticeName,
            providerName,
            "https://example.com/notice-" + id,
            null,
            null,
            null,
            noticeStatus,
            announcementDate,
            applicationStartDate,
            applicationEndDate,
            winnerAnnouncementDate,
            "서울특별시",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "match-" + id,
            false,
            null,
            favoritedDate.atStartOfDay()
        );
        return new UserFavoriteAnnouncement(1L, announcement);
    }
}
