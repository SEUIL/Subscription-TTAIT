package com.ttait.subscription.admin.service;

import com.ttait.subscription.admin.dto.RtmsCollectionAllRequest;
import com.ttait.subscription.admin.dto.RtmsCollectionAllResponse;
import com.ttait.subscription.admin.dto.RtmsCollectionRequest;
import com.ttait.subscription.admin.dto.RtmsCollectionResponse;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.market.service.MarketRtmsCollectionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AdminMarketCollectionService {

    private final MarketRtmsCollectionService marketRtmsCollectionService;

    public AdminMarketCollectionService(MarketRtmsCollectionService marketRtmsCollectionService) {
        this.marketRtmsCollectionService = marketRtmsCollectionService;
    }

    public RtmsCollectionResponse collectRtms(RtmsCollectionRequest request) {
        if (request == null || request.sourceType() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "sourceType is required");
        }

        MarketRtmsCollectionService.CollectionResult result = marketRtmsCollectionService.collect(
                request.sourceType(),
                request.lawdCd(),
                request.dealYm(),
                request.pageNoOrDefault(),
                request.numOfRowsOrDefault()
        );
        return new RtmsCollectionResponse(
                result.sourceType().name(),
                result.lawdCd(),
                result.dealYm(),
                result.status().name(),
                result.fetchedCount(),
                result.savedCount(),
                result.duplicateCount(),
                result.failedCount(),
                result.message()
        );
    }
    public RtmsCollectionAllResponse collectAllRtms(RtmsCollectionAllRequest request) {
        if (request == null || request.sourceType() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "sourceType is required");
        }

        MarketRtmsCollectionService.CollectionAllResult result = marketRtmsCollectionService.collectAll(
                request.sourceType(),
                request.lawdCd(),
                request.dealYm(),
                request.numOfRowsOrDefault(),
                request.maxPages()
        );
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

}
