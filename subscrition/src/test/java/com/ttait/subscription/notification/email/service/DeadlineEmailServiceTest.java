package com.ttait.subscription.notification.email.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.notification.email.domain.EmailNotificationType;
import com.ttait.subscription.notification.email.repository.EmailNotificationLogRepository;
import com.ttait.subscription.notification.email.sender.MessageSender;
import com.ttait.subscription.notification.favorite.domain.UserFavoriteAnnouncement;
import com.ttait.subscription.notification.favorite.repository.UserFavoriteAnnouncementRepository;
import com.ttait.subscription.user.domain.User;
import com.ttait.subscription.user.domain.enums.UserStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@ExtendWith(MockitoExtension.class)
class DeadlineEmailServiceTest {

    @Mock
    private UserFavoriteAnnouncementRepository favoriteRepository;
    @Mock
    private EmailNotificationLogRepository logRepository;
    @Mock
    private MessageSender messageSender;
    @Mock
    private TemplateEngine templateEngine;

    private DeadlineEmailService deadlineEmailService;
    private User user;

    @BeforeEach
    void setUp() {
        deadlineEmailService = new DeadlineEmailService(
            favoriteRepository, logRepository, messageSender, templateEngine);
        user = User.builder()
            .loginId("testUser")
            .email("test@example.com")
            .passwordHash("hash")
            .phone("010-0000-0000")
            .status(UserStatus.ACTIVE)
            .build();
    }

    private Announcement mockAnnouncement(Long id, LocalDate endDate) {
        Announcement a = mock(Announcement.class);
        // 모두 lenient: 코드 경로에 따라 일부만 호출되는 스텁이므로
        lenient().when(a.getId()).thenReturn(id);
        lenient().when(a.getApplicationEndDate()).thenReturn(endDate);
        lenient().when(a.getNoticeStatus()).thenReturn(AnnouncementStatus.OPEN);
        lenient().when(a.getNoticeName()).thenReturn("테스트공고");
        lenient().when(a.getProviderName()).thenReturn("LH");
        lenient().when(a.getRegionLevel1()).thenReturn("서울");
        return a;
    }

    private UserFavoriteAnnouncement mockFavorite(Announcement announcement) {
        UserFavoriteAnnouncement fav = mock(UserFavoriteAnnouncement.class);
        // given()을 nested 없이 바로 호출
        given(fav.getAnnouncement()).willReturn(announcement);
        return fav;
    }

    @Nested
    @DisplayName("sendFor")
    class SendFor {

        @Test
        @DisplayName("즐겨찾기가 없으면 이메일을 발송하지 않는다")
        void sendFor_noFavorites_skips() {
            given(favoriteRepository.findActiveByUserId(any())).willReturn(List.of());

            deadlineEmailService.sendFor(user, LocalDate.now());

            then(messageSender).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("마감 7일 전 공고에 DEADLINE_7 이메일을 발송한다")
        void sendFor_deadlineIn7Days_sendsEmail() {
            LocalDate today = LocalDate.of(2025, 6, 1);
            Announcement announcement = mockAnnouncement(10L, today.plusDays(7));
            UserFavoriteAnnouncement fav = mockFavorite(announcement);

            given(favoriteRepository.findActiveByUserId(any())).willReturn(List.of(fav));
            given(logRepository.existsByUserIdAndAnnouncementIdAndType(
                any(), eq(10L), eq(EmailNotificationType.DEADLINE_7))).willReturn(false);
            given(templateEngine.process(eq("email/deadline-reminder"), any(Context.class)))
                .willReturn("<html>email</html>");

            deadlineEmailService.sendFor(user, today);

            then(messageSender).should().send(eq("test@example.com"), anyString(), anyString());
            then(logRepository).should().save(any());
        }

        @Test
        @DisplayName("마감 3일 전 공고에 DEADLINE_3 이메일을 발송한다")
        void sendFor_deadlineIn3Days_sendsEmail() {
            LocalDate today = LocalDate.of(2025, 6, 1);
            Announcement announcement = mockAnnouncement(10L, today.plusDays(3));
            UserFavoriteAnnouncement fav = mockFavorite(announcement);

            given(favoriteRepository.findActiveByUserId(any())).willReturn(List.of(fav));
            given(logRepository.existsByUserIdAndAnnouncementIdAndType(
                any(), eq(10L), eq(EmailNotificationType.DEADLINE_3))).willReturn(false);
            given(templateEngine.process(eq("email/deadline-reminder"), any(Context.class)))
                .willReturn("<html>email</html>");

            deadlineEmailService.sendFor(user, today);

            then(messageSender).should().send(eq("test@example.com"), anyString(), anyString());
        }

        @Test
        @DisplayName("마감 1일 전 공고에 DEADLINE_1 이메일을 발송한다")
        void sendFor_deadlineIn1Day_sendsEmail() {
            LocalDate today = LocalDate.of(2025, 6, 1);
            Announcement announcement = mockAnnouncement(10L, today.plusDays(1));
            UserFavoriteAnnouncement fav = mockFavorite(announcement);

            given(favoriteRepository.findActiveByUserId(any())).willReturn(List.of(fav));
            given(logRepository.existsByUserIdAndAnnouncementIdAndType(
                any(), eq(10L), eq(EmailNotificationType.DEADLINE_1))).willReturn(false);
            given(templateEngine.process(eq("email/deadline-reminder"), any(Context.class)))
                .willReturn("<html>email</html>");

            deadlineEmailService.sendFor(user, today);

            then(messageSender).should().send(eq("test@example.com"), anyString(), anyString());
        }

        @Test
        @DisplayName("이미 발송된 타입은 중복 발송하지 않는다")
        void sendFor_alreadySentType_skips() {
            LocalDate today = LocalDate.of(2025, 6, 1);
            Announcement announcement = mockAnnouncement(10L, today.plusDays(7));
            UserFavoriteAnnouncement fav = mockFavorite(announcement);

            given(favoriteRepository.findActiveByUserId(any())).willReturn(List.of(fav));
            given(logRepository.existsByUserIdAndAnnouncementIdAndType(
                any(), eq(10L), eq(EmailNotificationType.DEADLINE_7))).willReturn(true);

            deadlineEmailService.sendFor(user, today);

            then(messageSender).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("applicationEndDate가 null인 공고는 건너뛴다")
        void sendFor_nullEndDate_skips() {
            Announcement announcement = mockAnnouncement(10L, null);
            UserFavoriteAnnouncement fav = mockFavorite(announcement);

            given(favoriteRepository.findActiveByUserId(any())).willReturn(List.of(fav));

            deadlineEmailService.sendFor(user, LocalDate.now());

            then(messageSender).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("마감일과 관계없는 날짜에는 이메일을 발송하지 않는다")
        void sendFor_nonTargetDate_skips() {
            LocalDate today = LocalDate.of(2025, 6, 1);
            Announcement announcement = mockAnnouncement(10L, today.plusDays(5)); // D-5
            UserFavoriteAnnouncement fav = mockFavorite(announcement);

            given(favoriteRepository.findActiveByUserId(any())).willReturn(List.of(fav));

            deadlineEmailService.sendFor(user, today);

            then(messageSender).shouldHaveNoInteractions();
        }
    }
}
