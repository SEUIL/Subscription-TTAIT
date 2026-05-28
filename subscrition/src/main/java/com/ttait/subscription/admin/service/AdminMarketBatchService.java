package com.ttait.subscription.admin.service;

import com.ttait.subscription.admin.dto.MarketRtmsSnapshotBatchRequest;
import com.ttait.subscription.admin.dto.MarketRtmsSnapshotBatchResponse;
import com.ttait.subscription.admin.dto.MarketSnapshotAggregateResponse;
import com.ttait.subscription.admin.dto.RtmsCollectionAllResponse;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.external.rtms.RtmsApiResult;
import com.ttait.subscription.market.domain.MarketSourceType;
import com.ttait.subscription.market.service.MarketPriceSnapshotAggregationService;
import com.ttait.subscription.market.service.MarketRtmsCollectionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AdminMarketBatchService {

    private final MarketRtmsCollectionService collectionService;
    private final MarketPriceSnapshotAggregationService aggregationService;

    public AdminMarketBatchService(MarketRtmsCollectionService collectionService,
                                   MarketPriceSnapshotAggregationService aggregationService) {
        this.collectionService = collectionService;
        this.aggregationService = aggregationService;
    }

    public MarketRtmsSnapshotBatchResponse collectRtmsAndAggregateSnapshot(MarketRtmsSnapshotBatchRequest request) {
        validate(request);
        MarketRtmsCollectionService.CollectionAllResult collectionResult = collectionService.collectAll(
                request.sourceType(),
                request.lawdCd(),
                request.dealYm(),
                request.numOfRowsOrDefault(),
                request.maxPages()
        );
        RtmsCollectionAllResponse collectionResponse = toCollectionResponse(collectionResult);
        if (collectionResult.status() == RtmsApiResult.Status.FAILED) {
            return new MarketRtmsSnapshotBatchResponse(
                    "COLLECTION_FAILED",
                    collectionResponse,
                    null,
                    false,
                    collectionResult.message()
            );
        }

        MarketPriceSnapshotAggregationService.AggregationResult snapshotResult = aggregationService.aggregate(
                MarketSourceType.valueOf(request.sourceType().name()),
                request.lawdCd(),
                request.dealYmFromOrDefault(),
                request.dealYmToOrDefault(),
                request.areaMin(),
                request.areaMax(),
                request.minimumSampleCount()
        );
        return new MarketRtmsSnapshotBatchResponse(
                "SUCCESS",
                collectionResponse,
                toSnapshotResponse(snapshotResult),
                true,
                collectionResult.message()
        );
    }

    private void validate(MarketRtmsSnapshotBatchRequest request) {
        if (request == null || request.sourceType() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "sourceType is required");
        }
        if (request.areaMin() == null || request.areaMax() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "areaMin and areaMax are required");
        }
    }

    private RtmsCollectionAllResponse toCollectionResponse(MarketRtmsCollectionService.CollectionAllResult result) {
        return new RtmsCollectionAllResponse(
                result.sourceType().name(),
                result.lawdCd(),
                result.dealYm(),
                result.status().name(),
                result.fetchedCount(),
                result.savedCount(),
                result.duplicateCount(),
                result.failedCount(),
                result.collectedPageCount(),
                result.totalCount(),
                result.message()
        );
    }

    private MarketSnapshotAggregateResponse toSnapshotResponse(
            MarketPriceSnapshotAggregationService.AggregationResult result) {
        return new MarketSnapshotAggregateResponse(
                result.snapshotId(),
                result.sourceType().name(),
                result.lawdCd(),
                result.dealYmFrom(),
                result.dealYmTo(),
                result.areaMin(),
                result.areaMax(),
                result.sampleCount(),
                result.avgDepositAmount(),
                result.medianDepositAmount(),
                result.avgMonthlyRentAmount(),
                result.medianMonthlyRentAmount(),
                result.avgTradeAmount(),
                result.medianTradeAmount(),
                result.status(),
                result.snapshotKey(),
                result.aggregatedAt()
        );
    }
}
