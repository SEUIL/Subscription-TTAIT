package com.ttait.subscription.notification.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.notification.favorite.domain.UserFavoriteAnnouncement;
import com.ttait.subscription.notification.favorite.dto.FavoriteResponse;
import com.ttait.subscription.notification.favorite.repository.UserFavoriteAnnouncementRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private UserFavoriteAnnouncementRepository favoriteRepository;

    @Mock
    private AnnouncementRepository announcementRepository;

    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteService(favoriteRepository, announcementRepository);
    }

    @Nested
    @DisplayName("add")
    class Add {

        @Test
        @DisplayName("이미 즐겨찾기한 경우 중복 저장하지 않는다")
        void add_alreadyExists_skips() {
            given(favoriteRepository.existsByUserIdAndAnnouncementId(1L, 10L)).willReturn(true);

            favoriteService.add(1L, 10L);

            then(announcementRepository).shouldHaveNoInteractions();
            then(favoriteRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("공고가 존재하지 않으면 404 예외를 던진다")
        void add_announcementNotFound_throws() {
            given(favoriteRepository.existsByUserIdAndAnnouncementId(1L, 99L)).willReturn(false);
            given(announcementRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> favoriteService.add(1L, 99L))
                .isInstanceOf(ApiException.class);
        }

        @Test
        @DisplayName("소프트 삭제된 공고는 404 예외를 던진다")
        void add_deletedAnnouncement_throws() {
            Announcement deleted = mock(Announcement.class);
            given(deleted.isDeleted()).willReturn(true);
            given(favoriteRepository.existsByUserIdAndAnnouncementId(1L, 10L)).willReturn(false);
            given(announcementRepository.findById(10L)).willReturn(Optional.of(deleted));

            assertThatThrownBy(() -> favoriteService.add(1L, 10L))
                .isInstanceOf(ApiException.class);
        }

        @Test
        @DisplayName("정상 공고를 즐겨찾기에 추가한다")
        void add_validAnnouncement_saves() {
            Announcement announcement = mock(Announcement.class);
            given(announcement.isDeleted()).willReturn(false);
            given(favoriteRepository.existsByUserIdAndAnnouncementId(1L, 10L)).willReturn(false);
            given(announcementRepository.findById(10L)).willReturn(Optional.of(announcement));

            favoriteService.add(1L, 10L);

            then(favoriteRepository).should().save(any(UserFavoriteAnnouncement.class));
        }
    }

    @Nested
    @DisplayName("remove")
    class Remove {

        @Test
        @DisplayName("즐겨찾기가 없으면 404 예외를 던진다")
        void remove_notFound_throws() {
            given(favoriteRepository.findByUserIdAndAnnouncementId(1L, 10L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> favoriteService.remove(1L, 10L))
                .isInstanceOf(ApiException.class);
        }

        @Test
        @DisplayName("즐겨찾기를 정상적으로 삭제한다")
        void remove_exists_deletes() {
            Announcement announcement = mock(Announcement.class);
            UserFavoriteAnnouncement fav = UserFavoriteAnnouncement.builder()
                .userId(1L).announcement(announcement).build();
            given(favoriteRepository.findByUserIdAndAnnouncementId(1L, 10L)).willReturn(Optional.of(fav));

            favoriteService.remove(1L, 10L);

            then(favoriteRepository).should().delete(fav);
        }
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("즐겨찾기 여부를 반환한다")
        void exists_returnsRepositoryResult() {
            given(favoriteRepository.existsByUserIdAndAnnouncementId(1L, 10L)).willReturn(true);

            assertThat(favoriteService.exists(1L, 10L)).isTrue();
        }
    }

    @Nested
    @DisplayName("list")
    class ListFavorites {

        @Test
        @DisplayName("즐겨찾기 목록을 FavoriteResponse로 변환해 반환한다")
        void list_returnsMappedPage() {
            Announcement announcement = mock(Announcement.class);
            given(announcement.getId()).willReturn(10L);
            given(announcement.getNoticeName()).willReturn("테스트공고");
            given(announcement.getNoticeStatus()).willReturn(AnnouncementStatus.OPEN);

            UserFavoriteAnnouncement fav = UserFavoriteAnnouncement.builder()
                .userId(1L).announcement(announcement).build();
            PageRequest pageable = PageRequest.of(0, 10);

            Page<UserFavoriteAnnouncement> page = new PageImpl<>(List.of(fav));
            given(favoriteRepository.findByUserIdWithAnnouncement(1L, pageable)).willReturn(page);

            Page<FavoriteResponse> result = favoriteService.list(1L, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).noticeName()).isEqualTo("테스트공고");
        }
    }
}
