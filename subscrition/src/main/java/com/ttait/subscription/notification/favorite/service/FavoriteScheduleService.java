package com.ttait.subscription.notification.favorite.service;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.ParseReviewStatus;
import com.ttait.subscription.notification.favorite.domain.UserFavoriteAnnouncement;
import com.ttait.subscription.notification.favorite.dto.FavoriteCalendarEventResponse;
import com.ttait.subscription.notification.favorite.dto.FavoriteScheduleGroupResponse;
import com.ttait.subscription.notification.favorite.dto.FavoriteScheduleItemResponse;
import com.ttait.subscription.notification.favorite.dto.FavoriteScheduleResponse;
import com.ttait.subscription.notification.favorite.dto.FavoriteScheduleStatus;
import com.ttait.subscription.notification.favorite.dto.FavoriteScheduleSummaryResponse;
import com.ttait.subscription.notification.favorite.repository.UserFavoriteAnnouncementRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FavoriteScheduleService {

    private static final String DISCLAIMER =
        "일정은 서비스에 저장된 공고 데이터를 기준으로 표시됩니다. 최종 신청 기간과 발표 일정은 공고 원문과 신청 사이트에서 반드시 확인해야 합니다.";

    private static final List<FavoriteScheduleStatus> GROUP_ORDER = List.of(
        FavoriteScheduleStatus.DUE_TODAY,
        FavoriteScheduleStatus.DUE_TOMORROW,
        FavoriteScheduleStatus.DUE_SOON,
        FavoriteScheduleStatus.OPEN,
        FavoriteScheduleStatus.UPCOMING,
        FavoriteScheduleStatus.DATE_UNKNOWN,
        FavoriteScheduleStatus.CLOSED
    );

    private final UserFavoriteAnnouncementRepository favoriteRepository;

    public FavoriteScheduleService(UserFavoriteAnnouncementRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    public FavoriteScheduleResponse getSchedule(Long userId) {
        return getSchedule(userId, LocalDate.now());
    }

    FavoriteScheduleResponse getSchedule(Long userId, LocalDate today) {
        List<UserFavoriteAnnouncement> favorites = favoriteRepository.findVisibleByUserIdWithAnnouncement(
            userId,
            ParseReviewStatus.publicVisibleStatuses());

        List<FavoriteScheduleItemResponse> items = favorites.stream()
            .map(favorite -> toItem(favorite, today))
            .toList();

        Map<FavoriteScheduleStatus, List<FavoriteScheduleItemResponse>> grouped = new EnumMap<>(FavoriteScheduleStatus.class);
        for (FavoriteScheduleStatus status : GROUP_ORDER) {
            grouped.put(status, new ArrayList<>());
        }
        for (FavoriteScheduleItemResponse item : items) {
            grouped.get(item.scheduleStatus()).add(item);
        }
        grouped.values().forEach(list -> list.sort(itemComparator(today)));

        List<FavoriteScheduleGroupResponse> groups = GROUP_ORDER.stream()
            .map(status -> new FavoriteScheduleGroupResponse(status.name(), label(status), grouped.get(status)))
            .toList();

        List<FavoriteCalendarEventResponse> calendarEvents = items.stream()
            .flatMap(item -> toCalendarEvents(item).stream())
            .sorted(calendarEventComparator())
            .toList();

        FavoriteScheduleSummaryResponse summary = new FavoriteScheduleSummaryResponse(
            items.size(),
            count(items, FavoriteScheduleStatus.DUE_TODAY),
            count(items, FavoriteScheduleStatus.DUE_TOMORROW),
            countDueWithinSevenDays(items),
            countOpen(items),
            count(items, FavoriteScheduleStatus.UPCOMING),
            count(items, FavoriteScheduleStatus.DATE_UNKNOWN),
            count(items, FavoriteScheduleStatus.CLOSED)
        );

        return new FavoriteScheduleResponse(summary, groups, calendarEvents, DISCLAIMER);
    }

    private FavoriteScheduleItemResponse toItem(UserFavoriteAnnouncement favorite, LocalDate today) {
        Announcement announcement = favorite.getAnnouncement();
        FavoriteScheduleStatus scheduleStatus = resolveStatus(announcement, today);
        Integer dDay = announcement.getApplicationEndDate() == null
            ? null
            : Math.toIntExact(ChronoUnit.DAYS.between(today, announcement.getApplicationEndDate()));

        return new FavoriteScheduleItemResponse(
            announcement.getId(),
            announcement.getNoticeName(),
            announcement.getProviderName(),
            announcement.getNoticeStatus().name(),
            announcement.getApplicationStartDate(),
            announcement.getApplicationEndDate(),
            announcement.getAnnouncementDate(),
            announcement.getWinnerAnnouncementDate(),
            favorite.getCreatedAt(),
            dDay,
            dDayLabel(scheduleStatus, dDay),
            scheduleStatus,
            statusMessage(scheduleStatus, announcement),
            "공고 확인"
        );
    }

    private FavoriteScheduleStatus resolveStatus(Announcement announcement, LocalDate today) {
        LocalDate startDate = announcement.getApplicationStartDate();
        LocalDate endDate = announcement.getApplicationEndDate();

        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return FavoriteScheduleStatus.DATE_UNKNOWN;
        }
        if (today.isAfter(endDate)) {
            return FavoriteScheduleStatus.CLOSED;
        }
        if (today.isBefore(startDate)) {
            return FavoriteScheduleStatus.UPCOMING;
        }

        long daysUntilEnd = ChronoUnit.DAYS.between(today, endDate);
        if (daysUntilEnd == 0) {
            return FavoriteScheduleStatus.DUE_TODAY;
        }
        if (daysUntilEnd == 1) {
            return FavoriteScheduleStatus.DUE_TOMORROW;
        }
        if (daysUntilEnd <= 7) {
            return FavoriteScheduleStatus.DUE_SOON;
        }
        return FavoriteScheduleStatus.OPEN;
    }

    private String dDayLabel(FavoriteScheduleStatus status, Integer dDay) {
        if (status == FavoriteScheduleStatus.DATE_UNKNOWN || dDay == null) {
            return null;
        }
        if (status == FavoriteScheduleStatus.CLOSED) {
            return "마감";
        }
        if (dDay == 0) {
            return "D-day";
        }
        return "D-" + dDay;
    }

    private String statusMessage(FavoriteScheduleStatus status, Announcement announcement) {
        return switch (status) {
            case DUE_TODAY -> "오늘 신청이 마감됩니다.";
            case DUE_TOMORROW -> "내일 신청이 마감됩니다.";
            case DUE_SOON -> "마감이 임박한 공고입니다.";
            case OPEN -> "현재 신청 기간입니다.";
            case UPCOMING -> announcement.getApplicationStartDate() + "부터 접수가 시작됩니다.";
            case DATE_UNKNOWN -> "신청 일정 확인이 필요합니다.";
            case CLOSED -> "신청이 마감된 공고입니다.";
        };
    }

    private List<FavoriteCalendarEventResponse> toCalendarEvents(FavoriteScheduleItemResponse item) {
        List<FavoriteCalendarEventResponse> events = new ArrayList<>();
        addCalendarEvent(events, item, item.announcementDate(), "ANNOUNCEMENT_DATE", "공고일", "LOW");
        addCalendarEvent(events, item, item.applicationStartDate(), "APPLICATION_START", "신청 시작", "NORMAL");
        addCalendarEvent(events, item, item.applicationEndDate(), "APPLICATION_END", "신청 마감", "HIGH");
        addCalendarEvent(events, item, item.winnerAnnouncementDate(), "WINNER_ANNOUNCEMENT", "당첨자 발표", "NORMAL");
        return events;
    }

    private void addCalendarEvent(List<FavoriteCalendarEventResponse> events,
                                  FavoriteScheduleItemResponse item,
                                  LocalDate date,
                                  String eventType,
                                  String prefix,
                                  String priority) {
        if (date == null) {
            return;
        }
        events.add(new FavoriteCalendarEventResponse(
            item.announcementId(),
            eventType,
            date,
            prefix + ": " + item.noticeName(),
            priority
        ));
    }

    private Comparator<FavoriteScheduleItemResponse> itemComparator(LocalDate today) {
        return Comparator
            .comparing((FavoriteScheduleItemResponse item) -> statusRank(item.scheduleStatus()))
            .thenComparing(item -> primaryDate(item, today), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(FavoriteScheduleItemResponse::favoritedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private LocalDate primaryDate(FavoriteScheduleItemResponse item, LocalDate today) {
        return switch (item.scheduleStatus()) {
            case UPCOMING -> item.applicationStartDate();
            case DATE_UNKNOWN -> item.favoritedAt() == null ? null : item.favoritedAt().toLocalDate();
            default -> item.applicationEndDate() == null ? today.plusYears(10) : item.applicationEndDate();
        };
    }

    private Comparator<FavoriteCalendarEventResponse> calendarEventComparator() {
        return Comparator
            .comparing(FavoriteCalendarEventResponse::date)
            .thenComparing(event -> eventPriorityRank(event.priority()))
            .thenComparing(FavoriteCalendarEventResponse::title);
    }

    private int count(List<FavoriteScheduleItemResponse> items, FavoriteScheduleStatus status) {
        return (int) items.stream().filter(item -> item.scheduleStatus() == status).count();
    }

    private int countDueWithinSevenDays(List<FavoriteScheduleItemResponse> items) {
        Set<FavoriteScheduleStatus> dueStatuses = Set.of(
            FavoriteScheduleStatus.DUE_TODAY,
            FavoriteScheduleStatus.DUE_TOMORROW,
            FavoriteScheduleStatus.DUE_SOON
        );
        return (int) items.stream()
            .filter(item -> dueStatuses.contains(item.scheduleStatus()))
            .count();
    }

    private int countOpen(List<FavoriteScheduleItemResponse> items) {
        Set<FavoriteScheduleStatus> openStatuses = Set.of(
            FavoriteScheduleStatus.DUE_TODAY,
            FavoriteScheduleStatus.DUE_TOMORROW,
            FavoriteScheduleStatus.DUE_SOON,
            FavoriteScheduleStatus.OPEN
        );
        return (int) items.stream()
            .filter(item -> openStatuses.contains(item.scheduleStatus()))
            .count();
    }

    private String label(FavoriteScheduleStatus status) {
        return switch (status) {
            case DUE_TODAY -> "오늘 마감";
            case DUE_TOMORROW -> "내일 마감";
            case DUE_SOON -> "7일 이내 마감";
            case OPEN -> "접수 중";
            case UPCOMING -> "접수 예정";
            case DATE_UNKNOWN -> "일정 확인 필요";
            case CLOSED -> "마감됨";
        };
    }

    private int statusRank(FavoriteScheduleStatus status) {
        return GROUP_ORDER.indexOf(status);
    }

    private int eventPriorityRank(String priority) {
        return switch (priority) {
            case "HIGH" -> 0;
            case "NORMAL" -> 1;
            default -> 2;
        };
    }
}
