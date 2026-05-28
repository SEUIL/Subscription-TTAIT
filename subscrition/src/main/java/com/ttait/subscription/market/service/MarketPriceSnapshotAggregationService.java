package com.ttait.subscription.market.service;

import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.external.support.CanonicalJsonHasher;
import com.ttait.subscription.market.domain.MarketPriceSnapshot;
import com.ttait.subscription.market.domain.MarketSnapshotStatus;
import com.ttait.subscription.market.domain.MarketSourceType;
import com.ttait.subscription.market.domain.MarketTransactionRaw;
import com.ttait.subscription.market.repository.MarketPriceSnapshotRepository;
import com.ttait.subscription.market.repository.MarketTransactionRawRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MarketPriceSnapshotAggregationService {

    private static final int DEFAULT_MINIMUM_SAMPLE_COUNT = 3;

    private final MarketTransactionRawRepository rawRepository;
    private final MarketPriceSnapshotRepository snapshotRepository;
    private final CanonicalJsonHasher hasher;

    public MarketPriceSnapshotAggregationService(MarketTransactionRawRepository rawRepository,
                                                 MarketPriceSnapshotRepository snapshotRepository,
                                                 CanonicalJsonHasher hasher) {
        this.rawRepository = rawRepository;
        this.snapshotRepository = snapshotRepository;
        this.hasher = hasher;
    }

    @Transactional
    public AggregationResult aggregate(MarketSourceType sourceType,
                                       String lawdCd,
                                       String dealYmFrom,
                                       String dealYmTo,
                                       BigDecimal areaMin,
                                       BigDecimal areaMax,
                                       Integer minimumSampleCount) {
        validate(sourceType, lawdCd, dealYmFrom, dealYmTo, areaMin, areaMax, minimumSampleCount);
        int resolvedMinimumSampleCount = minimumSampleCount == null
                ? DEFAULT_MINIMUM_SAMPLE_COUNT
                : minimumSampleCount;

        List<MarketTransactionRaw> rows = rawRepository.findBySourceTypeAndLawdCdAndDealYmBetweenAndExclusiveAreaBetween(
                sourceType,
                lawdCd,
                dealYmFrom,
                dealYmTo,
                areaMin,
                areaMax
        );
        int sampleCount = rows.size();
        MarketSnapshotStatus status = sampleCount >= resolvedMinimumSampleCount
                ? MarketSnapshotStatus.OK
                : MarketSnapshotStatus.INSUFFICIENT_DATA;
        LocalDateTime aggregatedAt = LocalDateTime.now();
        String snapshotKey = snapshotKey(sourceType, lawdCd, dealYmFrom, dealYmTo, areaMin, areaMax);

        snapshotRepository.findBySnapshotKey(snapshotKey).ifPresent(existing -> {
            snapshotRepository.delete(existing);
            snapshotRepository.flush();
        });

        MarketPriceSnapshot snapshot = snapshotRepository.save(MarketPriceSnapshot.builder()
                .sourceType(sourceType)
                .lawdCd(lawdCd)
                .dealYmFrom(dealYmFrom)
                .dealYmTo(dealYmTo)
                .areaMin(areaMin)
                .areaMax(areaMax)
                .sampleCount(sampleCount)
                .avgDepositAmount(average(rows, MarketTransactionRaw::getDepositAmount))
                .medianDepositAmount(median(rows, MarketTransactionRaw::getDepositAmount))
                .avgMonthlyRentAmount(average(rows, MarketTransactionRaw::getMonthlyRentAmount))
                .medianMonthlyRentAmount(median(rows, MarketTransactionRaw::getMonthlyRentAmount))
                .avgTradeAmount(average(rows, MarketTransactionRaw::getTradeAmount))
                .medianTradeAmount(median(rows, MarketTransactionRaw::getTradeAmount))
                .status(status)
                .snapshotKey(snapshotKey)
                .aggregatedAt(aggregatedAt)
                .build());

        return AggregationResult.from(snapshot);
    }

    private void validate(MarketSourceType sourceType,
                          String lawdCd,
                          String dealYmFrom,
                          String dealYmTo,
                          BigDecimal areaMin,
                          BigDecimal areaMax,
                          Integer minimumSampleCount) {
        if (sourceType == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "sourceType is required");
        }
        if (!StringUtils.hasText(lawdCd) || !lawdCd.matches("\\d{5}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "lawdCd must be 5 digits");
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
        if (areaMin == null || areaMax == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "areaMin and areaMax are required");
        }
        if (areaMin.compareTo(areaMax) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "areaMin must be less than or equal to areaMax");
        }
        if (minimumSampleCount != null && minimumSampleCount < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "minimumSampleCount must be greater than 0");
        }
    }

    private boolean validDealYm(String dealYm) {
        return StringUtils.hasText(dealYm) && dealYm.matches("\\d{6}");
    }

    private Long average(List<MarketTransactionRaw> rows, Function<MarketTransactionRaw, Long> mapper) {
        List<Long> values = longValues(rows, mapper);
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal sum = values.stream()
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 0, RoundingMode.HALF_UP).longValue();
    }

    private Long median(List<MarketTransactionRaw> rows, Function<MarketTransactionRaw, Long> mapper) {
        List<Long> values = longValues(rows, mapper).stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        if (values.isEmpty()) {
            return null;
        }
        int middle = values.size() / 2;
        if (values.size() % 2 == 1) {
            return values.get(middle);
        }
        return BigDecimal.valueOf(values.get(middle - 1))
                .add(BigDecimal.valueOf(values.get(middle)))
                .divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP)
                .longValue();
    }

    private List<Long> longValues(List<MarketTransactionRaw> rows, Function<MarketTransactionRaw, Long> mapper) {
        return rows.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .toList();
    }

    private String snapshotKey(MarketSourceType sourceType,
                               String lawdCd,
                               String dealYmFrom,
                               String dealYmTo,
                               BigDecimal areaMin,
                               BigDecimal areaMax) {
        return hasher.sha256Text(String.join("|",
                sourceType.name(),
                lawdCd,
                dealYmFrom,
                dealYmTo,
                normalizeArea(areaMin),
                normalizeArea(areaMax)
        ));
    }

    private String normalizeArea(BigDecimal area) {
        return area.stripTrailingZeros().toPlainString();
    }

    public record AggregationResult(
            Long snapshotId,
            MarketSourceType sourceType,
            String lawdCd,
            String dealYmFrom,
            String dealYmTo,
            BigDecimal areaMin,
            BigDecimal areaMax,
            int sampleCount,
            Long avgDepositAmount,
            Long medianDepositAmount,
            Long avgMonthlyRentAmount,
            Long medianMonthlyRentAmount,
            Long avgTradeAmount,
            Long medianTradeAmount,
            MarketSnapshotStatus status,
            String snapshotKey,
            LocalDateTime aggregatedAt
    ) {

        private static AggregationResult from(MarketPriceSnapshot snapshot) {
            return new AggregationResult(
                    snapshot.getId(),
                    snapshot.getSourceType(),
                    snapshot.getLawdCd(),
                    snapshot.getDealYmFrom(),
                    snapshot.getDealYmTo(),
                    snapshot.getAreaMin(),
                    snapshot.getAreaMax(),
                    snapshot.getSampleCount(),
                    snapshot.getAvgDepositAmount(),
                    snapshot.getMedianDepositAmount(),
                    snapshot.getAvgMonthlyRentAmount(),
                    snapshot.getMedianMonthlyRentAmount(),
                    snapshot.getAvgTradeAmount(),
                    snapshot.getMedianTradeAmount(),
                    snapshot.getStatus(),
                    snapshot.getSnapshotKey(),
                    snapshot.getAggregatedAt()
            );
        }
    }
}
