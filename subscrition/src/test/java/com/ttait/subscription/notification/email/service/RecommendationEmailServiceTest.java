package com.ttait.subscription.notification.email.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.ttait.subscription.announcement.dto.RecommendationItemResponse;
import com.ttait.subscription.announcement.service.RecommendationService;
import com.ttait.subscription.notification.email.domain.EmailNotificationLog;
import com.ttait.subscription.notification.email.domain.EmailNotificationType;
import com.ttait.subscription.notification.email.repository.EmailNotificationLogRepository;
import com.ttait.subscription.notification.email.sender.MessageSender;
import com.ttait.subscription.user.domain.User;
import com.ttait.subscription.user.domain.enums.UserStatus;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@ExtendWith(MockitoExtension.class)
class RecommendationEmailServiceTest {

    @Mock
    private RecommendationService recommendationService;
    @Mock
    private EmailNotificationLogRepository logRepository;
    @Mock
    private MessageSender messageSender;
    @Mock
    private TemplateEngine templateEngine;

    private RecommendationEmailService emailService;

    private User user;

    @BeforeEach
    void setUp() {
        emailService = new RecommendationEmailService(
            recommendationService, logRepository, messageSender, templateEngine);
        user = User.builder()
            .loginId("testUser")
            .email("test@example.com")
            .passwordHash("hash")
            .phone("010-0000-0000")
            .status(UserStatus.ACTIVE)
            .build();
    }

    private RecommendationItemResponse item(long id) {
        return new RecommendationItemResponse(
            id, "공고" + id, "LH", "공공임대", "아파트",
            "서울", "강남구", "서울 강남구", "테스트단지",
            1000L, 30L, null, null,
            "OPEN", "https://example.com/" + id, 80, List.of("청년")
        );
    }

    @Nested
    @DisplayName("sendFor")
    class SendFor {

        @Test
        @DisplayName("추천 공고가 없으면 이메일을 발송하지 않는다")
        void sendFor_emptyCandidates_skips() {
            given(recommendationService.getRecommendations(any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

            emailService.sendFor(user);

            then(messageSender).shouldHaveNoInteractions();
            then(logRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("모든 공고가 이미 발송된 경우 이메일을 발송하지 않는다")
        void sendFor_allAlreadySent_skips() {
            List<RecommendationItemResponse> candidates = List.of(item(1L), item(2L));
            given(recommendationService.getRecommendations(any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(candidates));
            given(logRepository.findAlreadySentAnnouncementIds(any(), any(), anyList()))
                .willReturn(Set.of(1L, 2L));

            emailService.sendFor(user);

            then(messageSender).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("신규 공고가 있으면 이메일을 발송하고 성공 로그를 저장한다")
        void sendFor_newItems_sendsEmailAndSavesSuccessLog() {
            List<RecommendationItemResponse> candidates = List.of(item(1L), item(2L));
            given(recommendationService.getRecommendations(any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(candidates));
            given(logRepository.findAlreadySentAnnouncementIds(any(), any(), anyList()))
                .willReturn(Set.of());
            given(templateEngine.process(eq("email/recommendation"), any(Context.class)))
                .willReturn("<html>email</html>");

            emailService.sendFor(user);

            then(messageSender).should().send(eq("test@example.com"), anyString(), anyString());
            then(logRepository).should().saveAll(anyList());
        }

        @Test
        @DisplayName("이메일 발송 실패 시 실패 로그를 저장하고 예외를 삼킨다")
        void sendFor_sendFailure_savesFailureLog() {
            List<RecommendationItemResponse> candidates = List.of(item(1L));
            given(recommendationService.getRecommendations(any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(candidates));
            given(logRepository.findAlreadySentAnnouncementIds(any(), any(), anyList()))
                .willReturn(Set.of());
            given(templateEngine.process(eq("email/recommendation"), any(Context.class)))
                .willReturn("<html>email</html>");
            willThrow(new RuntimeException("SMTP error")).given(messageSender)
                .send(anyString(), anyString(), anyString());

            emailService.sendFor(user);

            then(logRepository).should().saveAll(anyList());
        }

        @Test
        @DisplayName("추천 서비스 예외 발생 시 이메일 발송 없이 조용히 종료된다")
        void sendFor_recommendationServiceThrows_skips() {
            given(recommendationService.getRecommendations(any(), any(Pageable.class)))
                .willThrow(new RuntimeException("no profile"));

            emailService.sendFor(user);

            then(messageSender).shouldHaveNoInteractions();
            then(logRepository).shouldHaveNoInteractions();
        }
    }
}
