package com.example.demo.notification.service;

import com.example.demo.announcement.domain.Announcement;
import com.example.demo.announcement.repository.AnnouncementRepository;
import com.example.demo.common.exception.ApiException;
import com.example.demo.notification.domain.UserSavedAnnouncement;
import com.example.demo.notification.repository.UserSavedAnnouncementRepository;
import com.example.demo.user.domain.User;
import com.example.demo.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SavedAnnouncementService {

    private final UserSavedAnnouncementRepository userSavedAnnouncementRepository;
    private final UserRepository userRepository;
    private final AnnouncementRepository announcementRepository;

    public SavedAnnouncementService(UserSavedAnnouncementRepository userSavedAnnouncementRepository, UserRepository userRepository,
                                    AnnouncementRepository announcementRepository) {
        this.userSavedAnnouncementRepository = userSavedAnnouncementRepository;
        this.userRepository = userRepository;
        this.announcementRepository = announcementRepository;
    }

    public void save(Long userId, Long announcementId) {
        if (userSavedAnnouncementRepository.existsByUserIdAndAnnouncementId(userId, announcementId)) {
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user not found"));
        Announcement announcement = announcementRepository.findByIdAndDeletedFalse(announcementId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "announcement not found"));
        userSavedAnnouncementRepository.save(UserSavedAnnouncement.builder()
                .user(user)
                .announcement(announcement)
                .build());
    }

    public void unsave(Long userId, Long announcementId) {
        UserSavedAnnouncement saved = userSavedAnnouncementRepository.findByUserIdAndAnnouncementId(userId, announcementId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "saved announcement not found"));
        userSavedAnnouncementRepository.delete(saved);
    }
}
