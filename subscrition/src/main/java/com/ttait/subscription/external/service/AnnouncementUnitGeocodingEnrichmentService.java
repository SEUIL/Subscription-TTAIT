package com.ttait.subscription.external.service;

import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.announcement.domain.GeocodeStatus;
import com.ttait.subscription.announcement.repository.AnnouncementUnitRepository;
import com.ttait.subscription.external.naver.NaverGeocodingClient;
import com.ttait.subscription.external.naver.NaverGeocodingResult;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AnnouncementUnitGeocodingEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementUnitGeocodingEnrichmentService.class);
    private static final String NO_ADDRESS_MESSAGE = "주소 없음";
    private static final String NO_RESULT_MESSAGE = "검색 결과 없음";
    private static final String FAILED_MESSAGE = "geocoding 실패";
    private static final int MAX_MESSAGE_LENGTH = 500;

    private final AnnouncementUnitRepository announcementUnitRepository;
    private final NaverGeocodingClient naverGeocodingClient;

    public AnnouncementUnitGeocodingEnrichmentService(AnnouncementUnitRepository announcementUnitRepository,
                                                      NaverGeocodingClient naverGeocodingClient) {
        this.announcementUnitRepository = announcementUnitRepository;
        this.naverGeocodingClient = naverGeocodingClient;
    }

    public void enrichNotRequestedUnits(Long announcementId) {
        List<AnnouncementUnit> units = announcementUnitRepository
                .findByAnnouncementIdAndGeocodeStatusAndDeletedFalseOrderByUnitOrderAsc(
                        announcementId,
                        GeocodeStatus.NOT_REQUESTED
                );

        for (AnnouncementUnit unit : units) {
            enrichUnit(unit);
        }
    }

    private void enrichUnit(AnnouncementUnit unit) {
        LocalDateTime geocodedAt = LocalDateTime.now();
        String address = unit.getFullAddress();
        if (!StringUtils.hasText(address)) {
            unit.markGeocodeSkippedNoAddress(NO_ADDRESS_MESSAGE, geocodedAt);
            announcementUnitRepository.save(unit);
            return;
        }

        try {
            applyResult(unit, naverGeocodingClient.geocode(address), geocodedAt);
        } catch (RuntimeException e) {
            log.warn("Failed to geocode announcementUnitId={} cause={}", unit.getId(), e.getClass().getSimpleName());
            unit.markGeocodeFailed(FAILED_MESSAGE, geocodedAt);
        }
        announcementUnitRepository.save(unit);
    }

    private void applyResult(AnnouncementUnit unit, NaverGeocodingResult result, LocalDateTime geocodedAt) {
        switch (result.status()) {
            case SUCCESS -> unit.markGeocodeSuccess(result.latitude(), result.longitude(), geocodedAt);
            case NO_RESULT -> unit.markGeocodeNoResult(messageOrDefault(result.message(), NO_RESULT_MESSAGE), geocodedAt);
            case FAILED -> unit.markGeocodeFailed(messageOrDefault(result.message(), FAILED_MESSAGE), geocodedAt);
        }
    }

    private String messageOrDefault(String message, String defaultMessage) {
        String resolved = StringUtils.hasText(message) ? message : defaultMessage;
        if (resolved.length() <= MAX_MESSAGE_LENGTH) {
            return resolved;
        }
        return resolved.substring(0, MAX_MESSAGE_LENGTH);
    }
}
