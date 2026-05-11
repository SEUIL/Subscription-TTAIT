package com.example.demo.announcement.service;

import com.example.demo.announcement.domain.AnnouncementAttachment;
import com.example.demo.announcement.dto.AnnouncementDetailResponse;
import com.example.demo.announcement.dto.AnnouncementListItemResponse;
import com.example.demo.announcement.dto.AttachmentResponse;
import com.example.demo.announcement.dto.FilterOptionResponse;
import com.example.demo.announcement.dto.MarketComparisonResponse;
import com.example.demo.announcement.repository.AnnouncementAttachmentRepository;
import com.example.demo.announcement.repository.AnnouncementCategoryTagRepository;
import com.example.demo.announcement.repository.AnnouncementRepository;
import com.example.demo.announcement.repository.query.AnnouncementDetailRow;
import com.example.demo.announcement.repository.query.AnnouncementListRow;
import com.example.demo.announcement.repository.query.AnnouncementTagRow;
import com.example.demo.common.exception.ApiException;
import com.example.demo.market.service.MarketComparisonService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnnouncementQueryService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementAttachmentRepository announcementAttachmentRepository;
    private final AnnouncementCategoryTagRepository announcementCategoryTagRepository;
    private final MarketComparisonService marketComparisonService;

    public AnnouncementQueryService(AnnouncementRepository announcementRepository,
                                    AnnouncementAttachmentRepository announcementAttachmentRepository,
                                    AnnouncementCategoryTagRepository announcementCategoryTagRepository,
                                    MarketComparisonService marketComparisonService) {
        this.announcementRepository = announcementRepository;
        this.announcementAttachmentRepository = announcementAttachmentRepository;
        this.announcementCategoryTagRepository = announcementCategoryTagRepository;
        this.marketComparisonService = marketComparisonService;
    }

    public Page<AnnouncementListItemResponse> getAnnouncements(Long userId, String regionLevel1, String regionLevel2,
                                                               String supplyType, String provider, String status,
                                                               String keyword, String sort, Pageable pageable) {
        Page<AnnouncementListRow> page = announcementRepository.search(
                userId, regionLevel1, regionLevel2, supplyType, provider, status, keyword, sort, pageable
        );

        Map<Long, List<String>> tagsByAnnouncementId = fetchTags(page.getContent().stream()
                .map(AnnouncementListRow::announcementId)
                .toList());

        return page.map(row -> new AnnouncementListItemResponse(
                row.announcementId(),
                row.noticeName(),
                row.providerName(),
                row.supplyType(),
                row.houseType(),
                row.regionLevel1(),
                row.regionLevel2(),
                row.complexName(),
                row.depositAmount(),
                row.monthlyRentAmount(),
                row.applicationStartDate(),
                row.applicationEndDate(),
                row.noticeStatus().name(),
                tagsByAnnouncementId.getOrDefault(row.announcementId(), Collections.emptyList()),
                row.saved()
        ));
    }

    public AnnouncementDetailResponse getAnnouncementDetail(Long announcementId, Long userId) {
        AnnouncementDetailRow row = announcementRepository.findDetail(announcementId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "announcement not found"));

        List<AttachmentResponse> attachments = announcementAttachmentRepository.findByAnnouncementIdAndDeletedFalse(announcementId)
                .stream()
                .map(this::toAttachment)
                .toList();
        List<String> tags = fetchTags(List.of(announcementId)).getOrDefault(announcementId, Collections.emptyList());
        MarketComparisonResponse market = marketComparisonService.compare(row);

        return new AnnouncementDetailResponse(
                row.announcementId(),
                row.noticeName(),
                row.providerName(),
                row.noticeStatus().name(),
                row.announcementDate(),
                row.applicationStartDate(),
                row.applicationEndDate(),
                row.winnerAnnouncementDate(),
                row.supplyType(),
                row.houseType(),
                row.complexName(),
                row.fullAddress(),
                row.depositAmount(),
                row.monthlyRentAmount(),
                row.householdCount(),
                row.supplyHouseholdCount(),
                row.heatingType(),
                row.exclusiveAreaText(),
                row.exclusiveAreaValue(),
                row.moveInExpectedYm(),
                row.applicationDatetimeText(),
                row.guideText(),
                row.contactPhone(),
                attachments,
                row.sourceUrl(),
                market,
                tags
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

    private AttachmentResponse toAttachment(AnnouncementAttachment attachment) {
        return new AttachmentResponse(
                attachment.getAttachmentType().name(),
                attachment.getAttachmentName(),
                attachment.getAttachmentUrl()
        );
    }

    private Map<Long, List<String>> fetchTags(List<Long> announcementIds) {
        if (announcementIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, List<String>> tagsByAnnouncementId = new LinkedHashMap<>();
        for (AnnouncementTagRow row : announcementCategoryTagRepository.findTagRowsByAnnouncementIds(announcementIds)) {
            tagsByAnnouncementId.computeIfAbsent(row.announcementId(), ignored -> new ArrayList<>())
                    .add(row.categoryCode().name());
        }
        return tagsByAnnouncementId;
    }
}
