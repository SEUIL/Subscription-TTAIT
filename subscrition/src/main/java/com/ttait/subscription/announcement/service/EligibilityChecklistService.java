package com.ttait.subscription.announcement.service;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementEligibility;
import com.ttait.subscription.announcement.domain.ParseReviewStatus;
import com.ttait.subscription.announcement.dto.EligibilityChecklistResponse;
import com.ttait.subscription.announcement.repository.AnnouncementEligibilityRepository;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.user.domain.UserProfile;
import com.ttait.subscription.user.repository.UserProfileRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EligibilityChecklistService {

    private static final List<ParseReviewStatus> PUBLIC_VISIBLE_REVIEW_STATUSES = List.of(
            ParseReviewStatus.APPROVED,
            ParseReviewStatus.CORRECTED
    );

    private final UserProfileRepository userProfileRepository;
    private final AnnouncementRepository announcementRepository;
    private final AnnouncementEligibilityRepository announcementEligibilityRepository;
    private final EligibilityCheckEvaluator eligibilityCheckEvaluator;

    public EligibilityChecklistService(UserProfileRepository userProfileRepository,
                                       AnnouncementRepository announcementRepository,
                                       AnnouncementEligibilityRepository announcementEligibilityRepository,
                                       EligibilityCheckEvaluator eligibilityCheckEvaluator) {
        this.userProfileRepository = userProfileRepository;
        this.announcementRepository = announcementRepository;
        this.announcementEligibilityRepository = announcementEligibilityRepository;
        this.eligibilityCheckEvaluator = eligibilityCheckEvaluator;
    }

    public EligibilityChecklistResponse getChecklist(Long userId, Long announcementId) {
        UserProfile profile = userProfileRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "profile setup required"));
        Announcement announcement = announcementRepository.findPublicVisibleById(announcementId, PUBLIC_VISIBLE_REVIEW_STATUSES)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "announcement not found"));
        AnnouncementEligibility eligibility = announcementEligibilityRepository.findByAnnouncementId(announcementId)
                .orElse(null);
        return eligibilityCheckEvaluator.evaluate(profile, announcement, eligibility);
    }
}
