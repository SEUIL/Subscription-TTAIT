package com.ttait.subscription.announcement.service;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementCategory;
import com.ttait.subscription.announcement.domain.AnnouncementDetail;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.domain.ParseReviewStatus;
import com.ttait.subscription.announcement.dto.AnnouncementDetailResponse;
import com.ttait.subscription.announcement.dto.AnnouncementListItemResponse;
import com.ttait.subscription.announcement.dto.CategoryFilterOption;
import com.ttait.subscription.announcement.dto.CategoryFilterOptionResponse;
import com.ttait.subscription.announcement.dto.FilterOptionResponse;
import com.ttait.subscription.announcement.repository.AnnouncementCategoryRepository;
import com.ttait.subscription.announcement.repository.AnnouncementDetailRepository;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.announcement.repository.AnnouncementSearchCondition;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.external.support.AnnouncementNormalizer;
import com.ttait.subscription.user.domain.enums.CategoryCode;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class AnnouncementQueryService {

    private static final List<ParseReviewStatus> PUBLIC_VISIBLE_REVIEW_STATUSES = List.of(
            ParseReviewStatus.APPROVED,
            ParseReviewStatus.CORRECTED
    );

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementDetailRepository announcementDetailRepository;
    private final AnnouncementCategoryRepository announcementCategoryRepository;
    private final AnnouncementNormalizer announcementNormalizer;

    public AnnouncementQueryService(AnnouncementRepository announcementRepository,
                                     AnnouncementDetailRepository announcementDetailRepository,
                                     AnnouncementCategoryRepository announcementCategoryRepository,
                                     AnnouncementNormalizer announcementNormalizer) {
        this.announcementRepository = announcementRepository;
        this.announcementDetailRepository = announcementDetailRepository;
        this.announcementCategoryRepository = announcementCategoryRepository;
        this.announcementNormalizer = announcementNormalizer;
    }

    public Page<AnnouncementListItemResponse> getAnnouncements(String regionLevel1,
                                                               String regionLevel2,
                                                               String supplyType,
                                                               String houseType,
                                                               String provider,
                                                               String status,
                                                               String keyword,
                                                               Long minDeposit,
                                                               Long maxDeposit,
                                                               Long minMonthlyRent,
                                                               Long maxMonthlyRent,
                                                               List<CategoryCode> categories,
                                                               Pageable pageable) {
        AnnouncementStatus parsedStatus = parseStatus(status);
        AnnouncementSearchCondition condition = new AnnouncementSearchCondition(
                regionLevel1,
                regionLevel2,
                supplyType,
                normalizeHouseTypeFilter(houseType),
                provider,
                parsedStatus,
                keyword,
                minDeposit,
                maxDeposit,
                minMonthlyRent,
                maxMonthlyRent,
                categories,
                PUBLIC_VISIBLE_REVIEW_STATUSES
        );

        return announcementRepository.searchPublicVisible(condition, pageable)
                .map(this::toListItem);
    }

    public AnnouncementDetailResponse getAnnouncementDetail(Long announcementId) {
        Announcement announcement = announcementRepository.findPublicVisibleById(
                        announcementId,
                        PUBLIC_VISIBLE_REVIEW_STATUSES)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "announcement not found"));
        AnnouncementDetail detail = announcementDetailRepository.findByAnnouncementIdAndDeletedFalse(announcementId)
                .orElse(null);
        return toDetailResponse(announcement, detail);
    }

    public FilterOptionResponse regionLevel1Options() {
        return new FilterOptionResponse(
                announcementRepository.findDistinctPublicVisibleRegionLevel1(PUBLIC_VISIBLE_REVIEW_STATUSES));
    }

    public FilterOptionResponse regionLevel2Options(String regionLevel1) {
        List<Announcement> announcements = StringUtils.hasText(regionLevel1)
                ? announcementRepository.findPublicVisibleByRegionLevel1IgnoreCase(
                        regionLevel1,
                        PUBLIC_VISIBLE_REVIEW_STATUSES)
                : announcementRepository.findPublicVisible(PUBLIC_VISIBLE_REVIEW_STATUSES, Pageable.unpaged())
                        .getContent();

        List<String> items = announcements.stream()
                .map(this::resolveRegionLevel2)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        return new FilterOptionResponse(items);
    }

    public FilterOptionResponse supplyTypeOptions() {
        return new FilterOptionResponse(
                announcementRepository.findDistinctPublicVisibleSupplyTypes(PUBLIC_VISIBLE_REVIEW_STATUSES));
    }

    public FilterOptionResponse houseTypeOptions() {
        List<String> items = announcementRepository.findPublicVisible(PUBLIC_VISIBLE_REVIEW_STATUSES, Pageable.unpaged())
                .getContent()
                .stream()
                .map(this::resolveHouseType)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        return new FilterOptionResponse(items);
    }

    public FilterOptionResponse providerOptions() {
        return new FilterOptionResponse(
                announcementRepository.findDistinctPublicVisibleProviders(PUBLIC_VISIBLE_REVIEW_STATUSES));
    }

    public CategoryFilterOptionResponse categoryOptions() {
        return new CategoryFilterOptionResponse(List.of(
                new CategoryFilterOption(CategoryCode.YOUTH.name(), "청년"),
                new CategoryFilterOption(CategoryCode.NEWLYWED.name(), "신혼부부"),
                new CategoryFilterOption(CategoryCode.HOMELESS.name(), "무주택자"),
                new CategoryFilterOption(CategoryCode.ELDERLY.name(), "고령자"),
                new CategoryFilterOption(CategoryCode.LOW_INCOME.name(), "저소득자"),
                new CategoryFilterOption(CategoryCode.MULTI_CHILD.name(), "다자녀")
        ));
    }

    private AnnouncementListItemResponse toListItem(Announcement announcement) {
        String resolvedRegionLevel2 = resolveRegionLevel2(announcement);
        String resolvedHouseType = resolveHouseType(announcement);
        return new AnnouncementListItemResponse(
                announcement.getId(),
                announcement.getNoticeName(),
                announcement.getProviderName(),
                announcement.getSupplyTypeNormalized(),
                resolvedHouseType,
                announcement.getRegionLevel1(),
                resolvedRegionLevel2,
                announcement.getFullAddress(),
                announcement.getComplexName(),
                announcement.getDepositAmount(),
                announcement.getMonthlyRentAmount(),
                announcement.getApplicationStartDate(),
                announcement.getApplicationEndDate(),
                announcement.getNoticeStatus().name()
        );
    }

    private AnnouncementDetailResponse toDetailResponse(Announcement announcement, AnnouncementDetail detail) {
        return new AnnouncementDetailResponse(
                announcement.getId(),
                announcement.getNoticeName(),
                announcement.getProviderName(),
                announcement.getNoticeStatus().name(),
                announcement.getAnnouncementDate(),
                announcement.getApplicationStartDate(),
                announcement.getApplicationEndDate(),
                announcement.getWinnerAnnouncementDate(),
                announcement.getSupplyTypeNormalized(),
                resolveHouseType(announcement),
                announcement.getComplexName(),
                announcement.getFullAddress(),
                announcement.getDepositAmount(),
                announcement.getMonthlyRentAmount(),
                detail != null ? detail.getHouseholdCount() : null,
                announcement.getSupplyHouseholdCount(),
                detail != null ? detail.getHeatingType() : null,
                detail != null ? detail.getExclusiveAreaText() : null,
                detail != null ? detail.getExclusiveAreaValue() : null,
                detail != null ? detail.getMoveInExpectedYm() : null,
                detail != null ? detail.getApplicationDatetimeText() : null,
                detail != null ? detail.getGuideText() : null,
                detail != null ? detail.getContactPhone() : null,
                announcement.getSourceNoticeUrl()
        );
    }

    private List<Long> extractIds(List<Announcement> announcements) {
        return announcements.stream().map(Announcement::getId).toList();
    }

    private Map<Long, Set<CategoryCode>> loadCategoryMap(Collection<Long> announcementIds) {
        Map<Long, Set<CategoryCode>> categoryMap = new HashMap<>();
        if (announcementIds.isEmpty()) {
            return categoryMap;
        }

        for (AnnouncementCategory category : announcementCategoryRepository.findByAnnouncementIdIn(announcementIds)) {
            categoryMap.computeIfAbsent(category.getAnnouncement().getId(), ignored -> EnumSet.noneOf(CategoryCode.class))
                    .add(category.getCategoryCode());
        }
        return categoryMap;
    }

    private AnnouncementStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }

        try {
            return AnnouncementStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid status: " + status);
        }
    }

    private String normalizeHouseTypeFilter(String houseType) {
        return StringUtils.hasText(houseType) ? announcementNormalizer.normalizeHouseType(houseType) : houseType;
    }

    private String resolveHouseType(Announcement announcement) {
        if (StringUtils.hasText(announcement.getHouseTypeRaw())) {
            String normalizedFromRaw = announcementNormalizer.normalizeHouseType(announcement.getHouseTypeRaw());
            if (StringUtils.hasText(normalizedFromRaw)) {
                return normalizedFromRaw;
            }
        }

        return StringUtils.hasText(announcement.getHouseTypeNormalized()) ? announcement.getHouseTypeNormalized() : null;
    }

    private String resolveRegionLevel2(Announcement announcement) {
        String extractedFromRegion = extractRegionLevel2Token(announcement.getRegionLevel2(), announcement.getRegionLevel1());
        if (StringUtils.hasText(extractedFromRegion)) {
            return extractedFromRegion;
        }
        return extractRegionLevel2Token(announcement.getFullAddress(), announcement.getRegionLevel1());
    }

    private String extractRegionLevel2Token(String source, String regionLevel1) {
        if (!StringUtils.hasText(source)) {
            return null;
        }

        String normalizedSource = normalizeWhitespace(source);
        String[] tokens = normalizedSource.split(" ");
        if (tokens.length == 0) {
            return null;
        }

        int startIndex = 0;
        if (StringUtils.hasText(regionLevel1)) {
            String normalizedRegionLevel1 = normalizeRegionToken(regionLevel1);
            for (int index = 0; index < tokens.length; index++) {
                if (normalizedRegionLevel1.equals(normalizeRegionToken(tokens[index]))) {
                    startIndex = index + 1;
                    break;
                }
            }
        }

        for (int index = startIndex; index < tokens.length; index++) {
            String token = sanitizeRegionToken(tokens[index]);
            if (isLevel2RegionToken(token)) {
                return token;
            }
        }

        String fallback = sanitizeRegionToken(normalizedSource);
        return isLevel2RegionToken(fallback) ? fallback : null;
    }

    private String normalizeRegionToken(String value) {
        return sanitizeRegionToken(value).toLowerCase(Locale.ROOT);
    }

    private String sanitizeRegionToken(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return normalizeWhitespace(value).replaceAll("^[\\p{Punct}]+|[\\p{Punct}]+$", "");
    }

    private String normalizeWhitespace(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private boolean isLevel2RegionToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        return token.endsWith("시") || token.endsWith("군") || token.endsWith("구");
    }
}
