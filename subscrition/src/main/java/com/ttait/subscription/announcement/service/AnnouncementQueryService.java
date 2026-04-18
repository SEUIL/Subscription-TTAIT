package com.ttait.subscription.announcement.service;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementDetail;
import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import com.ttait.subscription.announcement.dto.CategoryFilterOption;
import com.ttait.subscription.announcement.dto.CategoryFilterOptionResponse;
import com.ttait.subscription.announcement.dto.AnnouncementDetailResponse;
import com.ttait.subscription.announcement.dto.AnnouncementListItemResponse;
import com.ttait.subscription.announcement.dto.FilterOptionResponse;
import com.ttait.subscription.announcement.repository.AnnouncementDetailRepository;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.user.domain.enums.CategoryCode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class AnnouncementQueryService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementDetailRepository announcementDetailRepository;

    public AnnouncementQueryService(AnnouncementRepository announcementRepository,
                                    AnnouncementDetailRepository announcementDetailRepository) {
        this.announcementRepository = announcementRepository;
        this.announcementDetailRepository = announcementDetailRepository;
    }

    public Page<AnnouncementListItemResponse> getAnnouncements(String regionLevel1,
                                                               String supplyType,
                                                               String provider,
                                                               String status,
                                                               List<CategoryCode> categories,
                                                               Pageable pageable) {
        List<AnnouncementListItemResponse> filtered = announcementRepository.findByDeletedFalseAndMergedFalse(Pageable.unpaged())
                .getContent()
                .stream()
                .filter(announcement -> matchesRegionLevel1(announcement, regionLevel1))
                .filter(announcement -> matchesSupplyType(announcement, supplyType))
                .filter(announcement -> matchesProvider(announcement, provider))
                .filter(announcement -> matchesStatus(announcement, status))
                .filter(announcement -> matchesCategories(announcement, categories))
                .sorted(Comparator
                        .comparing(Announcement::getApplicationEndDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Announcement::getId, Comparator.reverseOrder()))
                .map(this::toListItem)
                .toList();

        int start = (int) pageable.getOffset();
        if (start >= filtered.size()) {
            return new PageImpl<>(List.of(), pageable, filtered.size());
        }

        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    public AnnouncementDetailResponse getAnnouncementDetail(Long announcementId) {
        Announcement announcement = announcementRepository.findByIdAndDeletedFalse(announcementId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "announcement not found"));
        AnnouncementDetail detail = announcementDetailRepository.findByAnnouncementIdAndDeletedFalse(announcementId)
                .orElse(null);
        return toDetailResponse(announcement, detail);
    }

    private AnnouncementListItemResponse toListItem(Announcement a) {
        return new AnnouncementListItemResponse(
                a.getId(),
                a.getNoticeName(),
                a.getProviderName(),
                a.getSupplyTypeNormalized(),
                a.getHouseTypeNormalized(),
                a.getRegionLevel1(),
                a.getRegionLevel2(),
                a.getComplexName(),
                a.getDepositAmount(),
                a.getMonthlyRentAmount(),
                a.getApplicationStartDate(),
                a.getApplicationEndDate(),
                a.getNoticeStatus().name()
        );
    }

    private AnnouncementDetailResponse toDetailResponse(Announcement a, AnnouncementDetail d) {
        return new AnnouncementDetailResponse(
                a.getId(),
                a.getNoticeName(),
                a.getProviderName(),
                a.getNoticeStatus().name(),
                a.getAnnouncementDate(),
                a.getApplicationStartDate(),
                a.getApplicationEndDate(),
                a.getWinnerAnnouncementDate(),
                a.getSupplyTypeNormalized(),
                a.getHouseTypeNormalized(),
                a.getComplexName(),
                a.getFullAddress(),
                a.getDepositAmount(),
                a.getMonthlyRentAmount(),
                d != null ? d.getHouseholdCount() : null,
                a.getSupplyHouseholdCount(),
                d != null ? d.getHeatingType() : null,
                d != null ? d.getExclusiveAreaText() : null,
                d != null ? d.getExclusiveAreaValue() : null,
                d != null ? d.getMoveInExpectedYm() : null,
                d != null ? d.getApplicationDatetimeText() : null,
                d != null ? d.getGuideText() : null,
                d != null ? d.getContactPhone() : null,
                a.getSourceNoticeUrl()
        );
    }

    public FilterOptionResponse regionLevel1Options() {
        return new FilterOptionResponse(announcementRepository.findDistinctRegionLevel1());
    }

    public FilterOptionResponse supplyTypeOptions() {
        return new FilterOptionResponse(announcementRepository.findDistinctSupplyTypes());
    }

    public FilterOptionResponse providerOptions() {
        return new FilterOptionResponse(announcementRepository.findDistinctProviders());
    }

    public CategoryFilterOptionResponse categoryOptions() {
        return new CategoryFilterOptionResponse(List.of(
                new CategoryFilterOption(CategoryCode.YOUTH.name(), "청년"),
                new CategoryFilterOption(CategoryCode.NEWLYWED.name(), "신혼부부"),
                new CategoryFilterOption(CategoryCode.HOMELESS.name(), "무주택"),
                new CategoryFilterOption(CategoryCode.ELDERLY.name(), "고령자"),
                new CategoryFilterOption(CategoryCode.LOW_INCOME.name(), "저소득층"),
                new CategoryFilterOption(CategoryCode.MULTI_CHILD.name(), "다자녀")
        ));
    }

    private boolean matchesRegionLevel1(Announcement announcement, String regionLevel1) {
        return !StringUtils.hasText(regionLevel1) || regionLevel1.equals(announcement.getRegionLevel1());
    }

    private boolean matchesSupplyType(Announcement announcement, String supplyType) {
        return !StringUtils.hasText(supplyType) || supplyType.equals(announcement.getSupplyTypeNormalized());
    }

    private boolean matchesProvider(Announcement announcement, String provider) {
        return !StringUtils.hasText(provider) || provider.equals(announcement.getProviderName());
    }

    private boolean matchesStatus(Announcement announcement, String status) {
        if (!StringUtils.hasText(status)) {
            return true;
        }

        try {
            return announcement.getNoticeStatus() == AnnouncementStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid status: " + status);
        }
    }

    private boolean matchesCategories(Announcement announcement, List<CategoryCode> categories) {
        if (categories == null || categories.isEmpty()) {
            return true;
        }
        return categories.stream().anyMatch(category -> matchesCategory(announcement, category));
    }

    private boolean matchesCategory(Announcement announcement, CategoryCode category) {
        String text = joinSearchText(announcement);
        return switch (category) {
            case YOUTH -> containsAny(text, "청년", "대학생", "청년안심");
            case NEWLYWED -> containsAny(text, "신혼", "예비신혼", "혼인", "부부");
            case HOMELESS -> containsAny(text, "무주택");
            case ELDERLY -> containsAny(text, "고령자", "노인");
            case LOW_INCOME -> containsAny(text, "저소득", "수급자", "차상위");
            case MULTI_CHILD -> containsAny(text, "다자녀");
        };
    }

    private String joinSearchText(Announcement announcement) {
        return String.join(" ",
                safeLower(announcement.getNoticeName()),
                safeLower(announcement.getSupplyTypeRaw()),
                safeLower(announcement.getSupplyTypeNormalized()),
                safeLower(announcement.getHouseTypeRaw()),
                safeLower(announcement.getHouseTypeNormalized()),
                safeLower(announcement.getProviderName()));
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
