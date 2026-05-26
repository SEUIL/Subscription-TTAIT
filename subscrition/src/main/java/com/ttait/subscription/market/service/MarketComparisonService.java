package com.ttait.subscription.market.service;

import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.announcement.repository.AnnouncementUnitRepository;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.market.domain.MarketPriceSnapshot;
import com.ttait.subscription.market.domain.MarketSnapshotStatus;
import com.ttait.subscription.market.domain.MarketSourceType;
import com.ttait.subscription.market.dto.MarketComparisonResponse;
import com.ttait.subscription.market.dto.MarketComparisonStatus;
import com.ttait.subscription.market.repository.MarketPriceSnapshotRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class MarketComparisonService {

    private final AnnouncementUnitRepository unitRepository;
    private final MarketPriceSnapshotRepository snapshotRepository;

    public MarketComparisonService(AnnouncementUnitRepository unitRepository,
                                   MarketPriceSnapshotRepository snapshotRepository) {
        this.unitRepository = unitRepository;
        this.snapshotRepository = snapshotRepository;
    }

    public MarketComparisonResponse compare(Long announcementId,
                                            Long unitId,
                                            MarketSourceType sourceType,
                                            String dealYmFrom,
                                            String dealYmTo) {
        validateRequest(announcementId, unitId, sourceType, dealYmFrom, dealYmTo);
        AnnouncementUnit unit = unitRepository.findByIdAndAnnouncementIdAndDeletedFalse(unitId, announcementId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "announcement unit not found"));

        if (!StringUtils.hasText(unit.getLawdCd())) {
            return unavailable(unit, sourceType, dealYmFrom, dealYmTo,
                    MarketComparisonStatus.UNIT_LAWD_CD_MISSING,
                    "unit lawdCd is missing; run address normalization first");
        }
        if (unit.getExclusiveAreaValue() == null) {
            return unavailable(unit, sourceType, dealYmFrom, dealYmTo,
                    MarketComparisonStatus.UNIT_AREA_MISSING,
                    "unit exclusiveAreaValue is missing");
        }

        return snapshotRepository
                .findFirstBySourceTypeAndLawdCdAndDealYmFromAndDealYmToAndAreaMinLessThanEqualAndAreaMaxGreaterThanEqualOrderByAggregatedAtDesc(
                        sourceType,
                        unit.getLawdCd(),
                        dealYmFrom,
                        dealYmTo,
                        unit.getExclusiveAreaValue(),
                        unit.getExclusiveAreaValue())
                .map(snapshot -> comparable(unit, sourceType, dealYmFrom, dealYmTo, snapshot))
                .orElseGet(() -> unavailable(unit, sourceType, dealYmFrom, dealYmTo,
                        MarketComparisonStatus.SNAPSHOT_NOT_FOUND,
                        "matching market snapshot not found"));
    }

    private void validateRequest(Long announcementId,
                                 Long unitId,
                                 MarketSourceType sourceType,
                                 String dealYmFrom,
                                 String dealYmTo) {
        if (announcementId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "announcementId is required");
        }
        if (unitId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "unitId is required");
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
        if (dealYmFrom.compareTo(dealYmTo) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "dealYmFrom must be before or equal to dealYmTo");
        }
    }

    private boolean validDealYm(String dealYm) {
        return StringUtils.hasText(dealYm) && dealYm.matches("\\d{6}");
    }

    private MarketComparisonResponse unavailable(AnnouncementUnit unit,
                                                 MarketSourceType sourceType,
                                                 String dealYmFrom,
                                                 String dealYmTo,
                                                 MarketComparisonStatus status,
                                                 String message) {
        return new MarketComparisonResponse(
                unit.getAnnouncement().getId(),
                unit.getId(),
                sourceType.name(),
                unit.getLawdCd(),
                dealYmFrom,
                dealYmTo,
                unit.getExclusiveAreaValue(),
                status,
                message,
                unitPrice(unit),
                null,
                null,
                null,
                null
        );
    }

    private MarketComparisonResponse comparable(AnnouncementUnit unit,
                                                MarketSourceType sourceType,
                                                String dealYmFrom,
                                                String dealYmTo,
                                                MarketPriceSnapshot snapshot) {
        MarketComparisonStatus status = snapshot.getStatus() == MarketSnapshotStatus.OK
                ? MarketComparisonStatus.COMPARABLE
                : MarketComparisonStatus.INSUFFICIENT_DATA;
        String message = status == MarketComparisonStatus.COMPARABLE
                ? null
                : "snapshot sample count is below threshold";

        return new MarketComparisonResponse(
                unit.getAnnouncement().getId(),
                unit.getId(),
                sourceType.name(),
                unit.getLawdCd(),
                dealYmFrom,
                dealYmTo,
                unit.getExclusiveAreaValue(),
                status,
                message,
                unitPrice(unit),
                snapshotPrice(snapshot),
                difference(unit.getDepositAmount(), snapshot.getAvgDepositAmount()),
                difference(unit.getMonthlyRentAmount(), snapshot.getAvgMonthlyRentAmount()),
                difference(unitTradeAmount(unit), snapshot.getAvgTradeAmount())
        );
    }

    private MarketComparisonResponse.UnitPrice unitPrice(AnnouncementUnit unit) {
        return new MarketComparisonResponse.UnitPrice(
                unit.getDepositAmount(),
                unit.getMonthlyRentAmount(),
                unit.getSalePriceMin(),
                unit.getSalePriceMax()
        );
    }

    private MarketComparisonResponse.SnapshotPrice snapshotPrice(MarketPriceSnapshot snapshot) {
        return new MarketComparisonResponse.SnapshotPrice(
                snapshot.getId(),
                snapshot.getSampleCount(),
                snapshot.getStatus(),
                snapshot.getAreaMin(),
                snapshot.getAreaMax(),
                snapshot.getAvgDepositAmount(),
                snapshot.getMedianDepositAmount(),
                snapshot.getAvgMonthlyRentAmount(),
                snapshot.getMedianMonthlyRentAmount(),
                snapshot.getAvgTradeAmount(),
                snapshot.getMedianTradeAmount(),
                snapshot.getAggregatedAt()
        );
    }

    private Long unitTradeAmount(AnnouncementUnit unit) {
        if (unit.getSalePriceMin() == null) {
            return unit.getSalePriceMax();
        }
        if (unit.getSalePriceMax() == null) {
            return unit.getSalePriceMin();
        }
        return BigDecimal.valueOf(unit.getSalePriceMin())
                .add(BigDecimal.valueOf(unit.getSalePriceMax()))
                .divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP)
                .longValue();
    }

    private MarketComparisonResponse.PriceDifference difference(Long unitAmount, Long marketAmount) {
        if (unitAmount == null || marketAmount == null) {
            return null;
        }
        Long differenceAmount = unitAmount - marketAmount;
        BigDecimal differenceRatePercent = marketAmount == 0
                ? null
                : BigDecimal.valueOf(differenceAmount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(marketAmount), 2, RoundingMode.HALF_UP);
        return new MarketComparisonResponse.PriceDifference(
                unitAmount,
                marketAmount,
                differenceAmount,
                differenceRatePercent
        );
    }
}
