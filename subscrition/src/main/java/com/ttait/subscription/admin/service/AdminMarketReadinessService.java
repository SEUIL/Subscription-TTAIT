package com.ttait.subscription.admin.service;

import com.ttait.subscription.admin.dto.MarketReadinessResponse;
import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.announcement.repository.AnnouncementUnitRepository;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.external.rtms.RtmsProperties;
import com.ttait.subscription.market.domain.MarketPriceSnapshot;
import com.ttait.subscription.market.domain.MarketSnapshotStatus;
import com.ttait.subscription.market.domain.MarketSourceType;
import com.ttait.subscription.market.repository.MarketPriceSnapshotRepository;
import com.ttait.subscription.market.repository.MarketTransactionRawRepository;
import com.ttait.subscription.market.service.MarketSourceTypeResolver;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class AdminMarketReadinessService {

    private static final DateTimeFormatter DEAL_YM_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final int MAX_DEAL_MONTH_RANGE = 12;

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementUnitRepository unitRepository;
    private final MarketPriceSnapshotRepository snapshotRepository;
    private final MarketTransactionRawRepository rawRepository;
    private final RtmsProperties rtmsProperties;

    public AdminMarketReadinessService(AnnouncementRepository announcementRepository,
                                       AnnouncementUnitRepository unitRepository,
                                       MarketPriceSnapshotRepository snapshotRepository,
                                       MarketTransactionRawRepository rawRepository,
                                       RtmsProperties rtmsProperties) {
        this.announcementRepository = announcementRepository;
        this.unitRepository = unitRepository;
        this.snapshotRepository = snapshotRepository;
        this.rawRepository = rawRepository;
        this.rtmsProperties = rtmsProperties;
    }

    public MarketReadinessResponse getReadiness(Long announcementId,
                                                MarketSourceType sourceType,
                                                String dealYmFrom,
                                                String dealYmTo) {
        validate(announcementId, sourceType, dealYmFrom, dealYmTo);
        ensureAnnouncementExists(announcementId);
        List<MarketReadinessResponse.UnitReadiness> units = unitRepository
                .findWithAnnouncementByAnnouncementIdAndDeletedFalseOrderByUnitOrderAsc(announcementId)
                .stream()
                .map(unit -> toReadiness(unit, sourceType, dealYmFrom, dealYmTo))
                .toList();
        long readyCount = units.stream().filter(MarketReadinessResponse.UnitReadiness::marketReady).count();
        return new MarketReadinessResponse(
                announcementId,
                sourceType.name(),
                dealYmFrom,
                dealYmTo,
                StringUtils.hasText(rtmsProperties.serviceKey()),
                readyCount,
                units.size() - readyCount,
                units
        );
    }

    private MarketReadinessResponse.UnitReadiness toReadiness(AnnouncementUnit unit,
                                                               MarketSourceType sourceType,
                                                               String dealYmFrom,
                                                               String dealYmTo) {
        MarketSourceType recommendedSourceType = MarketSourceTypeResolver.resolve(unit, sourceType);
        if (!StringUtils.hasText(unit.getLawdCd())) {
            return unitReadiness(unit, recommendedSourceType, 0, false, null, false, "UNIT_LAWD_CD_MISSING");
        }
        if (unit.getExclusiveAreaValue() == null) {
            return unitReadiness(unit, recommendedSourceType, 0, false, null, false, "UNIT_AREA_MISSING");
        }

        long rawCount = rawRepository.countBySourceTypeAndLawdCdAndDealYmBetweenAndExclusiveAreaBetween(
                recommendedSourceType,
                unit.getLawdCd(),
                dealYmFrom,
                dealYmTo,
                unit.getExclusiveAreaValue(),
                unit.getExclusiveAreaValue()
        );
        MarketPriceSnapshot snapshot = snapshotRepository
                .findFirstBySourceTypeAndLawdCdAndDealYmFromAndDealYmToAndAreaMinLessThanEqualAndAreaMaxGreaterThanEqualOrderByAggregatedAtDesc(
                        recommendedSourceType,
                        unit.getLawdCd(),
                        dealYmFrom,
                        dealYmTo,
                        unit.getExclusiveAreaValue(),
                        unit.getExclusiveAreaValue())
                .orElse(null);
        if (snapshot == null) {
            return unitReadiness(unit, recommendedSourceType, rawCount, false, null, false, "SNAPSHOT_NOT_FOUND");
        }
        if (snapshot.getStatus() != MarketSnapshotStatus.OK) {
            return unitReadiness(unit, recommendedSourceType, rawCount, true, snapshot.getStatus(), false, "INSUFFICIENT_DATA");
        }
        return unitReadiness(unit, recommendedSourceType, rawCount, true, snapshot.getStatus(), true, "READY");
    }

    private MarketReadinessResponse.UnitReadiness unitReadiness(AnnouncementUnit unit,
                                                                MarketSourceType sourceType,
                                                                long rawCount,
                                                                boolean snapshotFound,
                                                                MarketSnapshotStatus snapshotStatus,
                                                                boolean marketReady,
                                                                String blocker) {
        return new MarketReadinessResponse.UnitReadiness(
                unit.getId(),
                unit.getUnitOrder(),
                unit.getComplexName(),
                unit.getFullAddress(),
                unit.getLegalDongCode(),
                unit.getLawdCd(),
                unit.getAddressStatus() != null ? unit.getAddressStatus().name() : null,
                unit.getAddressMessage(),
                unit.getAddressNormalizedAt(),
                unit.getExclusiveAreaValue(),
                sourceType.name(),
                rawCount,
                snapshotFound,
                snapshotStatus,
                marketReady,
                blocker
        );
    }

    private void validate(Long announcementId, MarketSourceType sourceType, String dealYmFrom, String dealYmTo) {
        if (announcementId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "announcementId is required");
        }
        if (sourceType == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "sourceType is required");
        }
        if (!validDealYm(dealYmFrom)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "dealYmFrom must be YYYYMM");
        }
        if (!validDealYm(dealYmTo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "dealYmTo must be YYYYMM");
        }
        YearMonth from = parseDealYm(dealYmFrom, "dealYmFrom");
        YearMonth to = parseDealYm(dealYmTo, "dealYmTo");
        if (from.isAfter(to)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "dealYmFrom must be before or equal to dealYmTo");
        }
        if (ChronoUnit.MONTHS.between(from, to) + 1 > MAX_DEAL_MONTH_RANGE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "dealYm range must be within 12 months");
        }
    }

    private void ensureAnnouncementExists(Long announcementId) {
        if (!announcementRepository.existsById(announcementId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "announcement not found");
        }
    }

    private boolean validDealYm(String dealYm) {
        return StringUtils.hasText(dealYm) && dealYm.matches("\\d{6}");
    }

    private YearMonth parseDealYm(String dealYm, String fieldName) {
        try {
            return YearMonth.parse(dealYm, DEAL_YM_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, fieldName + " must be a valid YYYYMM");
        }
    }
}
