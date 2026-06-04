package com.ttait.subscription.admin.service;

import com.ttait.subscription.admin.dto.AddressNormalizationResponse;
import com.ttait.subscription.admin.dto.MarketPrepareRequest;
import com.ttait.subscription.admin.dto.MarketPrepareResponse;
import com.ttait.subscription.admin.dto.MarketRtmsSnapshotBatchRequest;
import com.ttait.subscription.admin.dto.MarketRtmsSnapshotBatchResponse;
import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.announcement.repository.AnnouncementUnitRepository;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.external.rtms.RtmsSourceType;
import com.ttait.subscription.market.domain.MarketSourceType;
import com.ttait.subscription.market.service.MarketSourceTypeResolver;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminMarketPrepareService {

    private static final DateTimeFormatter DEAL_YM_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final int MAX_DEAL_MONTH_RANGE = 12;
    private static final int MAX_NUM_OF_ROWS = 100;
    private static final int MAX_PAGES = 10;
    private static final int MAX_BATCH_COUNT = 20;

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementUnitRepository unitRepository;
    private final AdminMarketAddressService addressService;
    private final AdminMarketBatchService batchService;

    public AdminMarketPrepareService(AnnouncementRepository announcementRepository,
                                     AnnouncementUnitRepository unitRepository,
                                     AdminMarketAddressService addressService,
                                     AdminMarketBatchService batchService) {
        this.announcementRepository = announcementRepository;
        this.unitRepository = unitRepository;
        this.addressService = addressService;
        this.batchService = batchService;
    }

    public MarketPrepareResponse prepare(Long announcementId, MarketPrepareRequest request) {
        validate(announcementId, request);
        ensureAnnouncementExists(announcementId);
        AddressNormalizationResponse normalization = null;
        if (request.retryNoLawdCodeOrDefault()) {
            normalization = addressService.normalizeAnnouncementUnits(announcementId, true);
        }

        List<MarketPrepareResponse.UnitPreparation> units = new ArrayList<>();
        Map<BatchKey, MarketRtmsSnapshotBatchRequest> batchRequests = new LinkedHashMap<>();
        for (AnnouncementUnit unit : unitRepository.findWithAnnouncementByAnnouncementIdAndDeletedFalseOrderByUnitOrderAsc(announcementId)) {
            RtmsSourceType sourceType = resolveSourceType(unit, request.sourceTypeOrDefault());
            if (!StringUtils.hasText(unit.getLawdCd())) {
                units.add(unitPreparation(unit, sourceType, null, null, "SKIPPED", "UNIT_LAWD_CD_MISSING"));
                continue;
            }
            if (unit.getExclusiveAreaValue() == null) {
                units.add(unitPreparation(unit, sourceType, null, null, "SKIPPED", "UNIT_AREA_MISSING"));
                continue;
            }
            BigDecimal area = unit.getExclusiveAreaValue();
            MarketRtmsSnapshotBatchRequest batchRequest = new MarketRtmsSnapshotBatchRequest(
                    sourceType,
                    unit.getLawdCd(),
                    request.dealYm(),
                    request.numOfRowsOrDefault(),
                    maxPagesOrDefault(request),
                    request.dealYmFromOrDefault(),
                    request.dealYmToOrDefault(),
                    area,
                    area,
                    request.minimumSampleCount()
            );
            batchRequests.putIfAbsent(BatchKey.from(batchRequest), batchRequest);
            if (batchRequests.size() > MAX_BATCH_COUNT) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "prepare batch count must be 20 or less");
            }
            units.add(unitPreparation(unit, sourceType, area, area, "QUEUED", null));
        }

        List<MarketPrepareResponse.BatchPreparation> batches = batchRequests.values().stream()
                .map(this::runBatch)
                .toList();
        String status = batches.stream().anyMatch(batch -> !"SUCCESS".equals(batch.result().status()))
                ? "PARTIAL_SUCCESS"
                : "SUCCESS";
        if (batches.isEmpty()) {
            status = "NO_ELIGIBLE_UNITS";
        }
        int skipped = (int) units.stream().filter(unit -> "SKIPPED".equals(unit.status())).count();
        return new MarketPrepareResponse(
                announcementId,
                status,
                normalization,
                batches.size(),
                skipped,
                units,
                batches
        );
    }

    private MarketPrepareResponse.BatchPreparation runBatch(MarketRtmsSnapshotBatchRequest request) {
        MarketRtmsSnapshotBatchResponse result = batchService.collectRtmsAndAggregateSnapshot(request);
        return new MarketPrepareResponse.BatchPreparation(
                request.sourceType().name(),
                request.lawdCd(),
                request.dealYm(),
                request.dealYmFromOrDefault(),
                request.dealYmToOrDefault(),
                request.areaMin(),
                request.areaMax(),
                result
        );
    }

    private MarketPrepareResponse.UnitPreparation unitPreparation(AnnouncementUnit unit,
                                                                  RtmsSourceType sourceType,
                                                                  BigDecimal areaMin,
                                                                  BigDecimal areaMax,
                                                                  String status,
                                                                  String blocker) {
        return new MarketPrepareResponse.UnitPreparation(
                unit.getId(),
                sourceType.name(),
                unit.getLawdCd(),
                areaMin,
                areaMax,
                status,
                blocker
        );
    }

    private void validate(Long announcementId, MarketPrepareRequest request) {
        if (announcementId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "announcementId is required");
        }
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "request is required");
        }
        if (!validDealYm(request.dealYm())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "dealYm must be YYYYMM");
        }
        if (!validDealYm(request.dealYmFromOrDefault())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "dealYmFrom must be YYYYMM");
        }
        if (!validDealYm(request.dealYmToOrDefault())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "dealYmTo must be YYYYMM");
        }
        YearMonth dealYm = parseDealYm(request.dealYm(), "dealYm");
        YearMonth from = parseDealYm(request.dealYmFromOrDefault(), "dealYmFrom");
        YearMonth to = parseDealYm(request.dealYmToOrDefault(), "dealYmTo");
        if (from.isAfter(to)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "dealYmFrom must be before or equal to dealYmTo");
        }
        if (ChronoUnit.MONTHS.between(from, to) + 1 > MAX_DEAL_MONTH_RANGE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "dealYm range must be within 12 months");
        }
        if (dealYm.isBefore(from) || dealYm.isAfter(to)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "dealYm must be within dealYmFrom and dealYmTo");
        }
        if (request.numOfRowsOrDefault() < 1 || request.numOfRowsOrDefault() > MAX_NUM_OF_ROWS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "numOfRows must be between 1 and 100");
        }
        if (maxPagesOrDefault(request) < 1 || maxPagesOrDefault(request) > MAX_PAGES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "maxPages must be between 1 and 10");
        }
    }

    private void ensureAnnouncementExists(Long announcementId) {
        if (!announcementRepository.existsById(announcementId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "announcement not found");
        }
    }

    private int maxPagesOrDefault(MarketPrepareRequest request) {
        return request.maxPages() == null ? MAX_PAGES : request.maxPages();
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

    private RtmsSourceType resolveSourceType(AnnouncementUnit unit, RtmsSourceType requestedSourceType) {
        MarketSourceType requested = MarketSourceType.valueOf(requestedSourceType.name());
        MarketSourceType sourceType = MarketSourceTypeResolver.resolve(unit, requested);
        return RtmsSourceType.valueOf(sourceType.name());
    }

    private record BatchKey(String sourceType,
                            String lawdCd,
                            String dealYm,
                            String dealYmFrom,
                            String dealYmTo,
                            BigDecimal areaMin,
                            BigDecimal areaMax) {
        private static BatchKey from(MarketRtmsSnapshotBatchRequest request) {
            return new BatchKey(
                    request.sourceType().name(),
                    request.lawdCd(),
                    request.dealYm(),
                    request.dealYmFromOrDefault(),
                    request.dealYmToOrDefault(),
                    request.areaMin(),
                    request.areaMax()
            );
        }
    }
}
