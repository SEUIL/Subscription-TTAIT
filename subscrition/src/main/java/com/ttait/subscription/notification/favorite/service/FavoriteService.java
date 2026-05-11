package com.ttait.subscription.notification.favorite.service;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.ParseReviewStatus;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.notification.favorite.domain.UserFavoriteAnnouncement;
import com.ttait.subscription.notification.favorite.dto.FavoriteResponse;
import com.ttait.subscription.notification.favorite.repository.UserFavoriteAnnouncementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FavoriteService {

    private static final java.util.List<ParseReviewStatus> PUBLIC_VISIBLE_REVIEW_STATUSES = java.util.List.of(
            ParseReviewStatus.APPROVED,
            ParseReviewStatus.CORRECTED
    );

    private final UserFavoriteAnnouncementRepository favoriteRepository;
    private final AnnouncementRepository announcementRepository;

    public FavoriteService(UserFavoriteAnnouncementRepository favoriteRepository,
                           AnnouncementRepository announcementRepository) {
        this.favoriteRepository = favoriteRepository;
        this.announcementRepository = announcementRepository;
    }

    public void add(Long userId, Long announcementId) {
        Announcement announcement = announcementRepository.findPublicVisibleById(
                announcementId,
                PUBLIC_VISIBLE_REVIEW_STATUSES)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "announcement not found: " + announcementId));
        if (favoriteRepository.existsByUserIdAndAnnouncementId(userId, announcementId)) {
            return;
        }
        favoriteRepository.save(UserFavoriteAnnouncement.builder()
            .userId(userId)
            .announcement(announcement)
            .build());
    }

    public void remove(Long userId, Long announcementId) {
        UserFavoriteAnnouncement favorite = favoriteRepository
            .findByUserIdAndAnnouncementId(userId, announcementId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "favorite not found"));
        favoriteRepository.delete(favorite);
    }

    @Transactional(readOnly = true)
    public Page<FavoriteResponse> list(Long userId, Pageable pageable) {
        return favoriteRepository.findVisibleByUserIdWithAnnouncement(
                userId,
                PUBLIC_VISIBLE_REVIEW_STATUSES,
                pageable)
            .map(FavoriteResponse::from);
    }

    @Transactional(readOnly = true)
    public boolean exists(Long userId, Long announcementId) {
        return favoriteRepository.existsVisibleByUserIdAndAnnouncementId(
                userId,
                announcementId,
                PUBLIC_VISIBLE_REVIEW_STATUSES);
    }
}
