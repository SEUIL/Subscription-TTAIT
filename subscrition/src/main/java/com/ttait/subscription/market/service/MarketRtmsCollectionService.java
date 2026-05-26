package com.ttait.subscription.market.service;

import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.external.rtms.RtmsApiResult;
import com.ttait.subscription.external.rtms.RtmsClient;
import com.ttait.subscription.external.rtms.RtmsSourceType;
import com.ttait.subscription.external.rtms.RtmsTransactionItem;
import com.ttait.subscription.external.support.CanonicalJsonHasher;
import com.ttait.subscription.market.domain.MarketSourceType;
import com.ttait.subscription.market.domain.MarketTransactionRaw;
import com.ttait.subscription.market.repository.MarketTransactionRawRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketRtmsCollectionService {

    private static final int DEFAULT_MAX_PAGES = 100;

    private final RtmsClient rtmsClient;
    private final MarketTransactionRawRepository rawRepository;
    private final CanonicalJsonHasher hasher;

    public MarketRtmsCollectionService(RtmsClient rtmsClient,
                                       MarketTransactionRawRepository rawRepository,
                                       CanonicalJsonHasher hasher) {
        this.rtmsClient = rtmsClient;
        this.rawRepository = rawRepository;
        this.hasher = hasher;
    }

    @Transactional
    public CollectionResult collect(RtmsSourceType sourceType, String lawdCd, String dealYm, int pageNo, int numOfRows) {
        RtmsApiResult result = rtmsClient.fetch(sourceType, lawdCd, dealYm, pageNo, numOfRows);
        if (result.status() == RtmsApiResult.Status.NO_RESULT) {
            return CollectionResult.noResult(sourceType, lawdCd, dealYm, result.message());
        }
        if (result.status() == RtmsApiResult.Status.FAILED) {
            return CollectionResult.failed(sourceType, lawdCd, dealYm, result.message());
        }

        SaveCounts counts = saveItems(result.items(), sourceType, lawdCd, dealYm);
        return new CollectionResult(
                sourceType,
                lawdCd,
                dealYm,
                RtmsApiResult.Status.SUCCESS,
                counts.fetchedCount(),
                counts.savedCount(),
                counts.duplicateCount(),
                0,
                result.message()
        );
    }

    @Transactional
    public CollectionAllResult collectAll(RtmsSourceType sourceType,
                                          String lawdCd,
                                          String dealYm,
                                          int numOfRows,
                                          Integer maxPages) {
        int resolvedMaxPages = maxPages == null ? DEFAULT_MAX_PAGES : maxPages;
        if (resolvedMaxPages < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "maxPages must be positive");
        }

        int fetchedCount = 0;
        int savedCount = 0;
        int duplicateCount = 0;
        int collectedPageCount = 0;
        Integer totalCount = null;
        String lastMessage = null;

        for (int pageNo = 1; pageNo <= resolvedMaxPages; pageNo++) {
            RtmsApiResult result = rtmsClient.fetch(sourceType, lawdCd, dealYm, pageNo, numOfRows);
            if (result.status() == RtmsApiResult.Status.FAILED) {
                return CollectionAllResult.failed(
                        sourceType,
                        lawdCd,
                        dealYm,
                        fetchedCount,
                        savedCount,
                        duplicateCount,
                        collectedPageCount,
                        totalCount,
                        result.message()
                );
            }
            if (result.totalCount() != null) {
                totalCount = result.totalCount();
            }
            if (result.status() == RtmsApiResult.Status.NO_RESULT) {
                lastMessage = result.message();
                if (collectedPageCount == 0) {
                    return CollectionAllResult.noResult(sourceType, lawdCd, dealYm, totalCount, lastMessage);
                }
                break;
            }

            SaveCounts counts = saveItems(result.items(), sourceType, lawdCd, dealYm);
            fetchedCount += counts.fetchedCount();
            savedCount += counts.savedCount();
            duplicateCount += counts.duplicateCount();
            collectedPageCount++;
            lastMessage = result.message();

            if (shouldStop(pageNo, numOfRows, totalCount, result.items().size())) {
                break;
            }
        }

        return new CollectionAllResult(
                sourceType,
                lawdCd,
                dealYm,
                RtmsApiResult.Status.SUCCESS,
                fetchedCount,
                savedCount,
                duplicateCount,
                0,
                collectedPageCount,
                totalCount,
                lastMessage
        );
    }

    private boolean shouldStop(int pageNo, int numOfRows, Integer totalCount, int fetchedPageCount) {
        if (totalCount != null && pageNo * numOfRows >= totalCount) {
            return true;
        }
        return fetchedPageCount < numOfRows;
    }

    private SaveCounts saveItems(Iterable<RtmsTransactionItem> items,
                                 RtmsSourceType sourceType,
                                 String lawdCd,
                                 String dealYm) {
        int fetchedCount = 0;
        int savedCount = 0;
        int duplicateCount = 0;
        for (RtmsTransactionItem item : items) {
            fetchedCount++;
            String rawPayloadHash = rawPayloadHash(sourceType.name(), lawdCd, dealYm, item.rawPayload());
            if (rawRepository.existsByRawPayloadHash(rawPayloadHash)) {
                duplicateCount++;
                continue;
            }
            rawRepository.save(toRawTransaction(item, sourceType, lawdCd, dealYm, rawPayloadHash));
            savedCount++;
        }
        return new SaveCounts(fetchedCount, savedCount, duplicateCount);
    }

    private MarketTransactionRaw toRawTransaction(RtmsTransactionItem item,
                                                  RtmsSourceType sourceType,
                                                  String lawdCd,
                                                  String dealYm,
                                                  String rawPayloadHash) {
        return MarketTransactionRaw.builder()
                .sourceType(MarketSourceType.valueOf(sourceType.name()))
                .lawdCd(lawdCd)
                .dealYm(dealYm)
                .legalDongName(item.legalDongName())
                .buildingName(item.buildingName())
                .jibun(item.jibun())
                .roadName(item.roadName())
                .buildYear(item.buildYear())
                .exclusiveArea(item.exclusiveArea())
                .floor(item.floor())
                .depositAmount(item.depositAmount())
                .monthlyRentAmount(item.monthlyRentAmount())
                .tradeAmount(item.tradeAmount())
                .rawPayloadHash(rawPayloadHash)
                .rawPayload(item.rawPayload())
                .build();
    }

    private String rawPayloadHash(String sourceType, String lawdCd, String dealYm, String rawPayload) {
        return hasher.sha256Text(sourceType + "|" + lawdCd + "|" + dealYm + "|" + rawPayload);
    }

    private record SaveCounts(int fetchedCount, int savedCount, int duplicateCount) {
    }

    public record CollectionResult(
            RtmsSourceType sourceType,
            String lawdCd,
            String dealYm,
            RtmsApiResult.Status status,
            int fetchedCount,
            int savedCount,
            int duplicateCount,
            int failedCount,
            String message
    ) {

        private static CollectionResult noResult(RtmsSourceType sourceType, String lawdCd, String dealYm, String message) {
            return new CollectionResult(sourceType, lawdCd, dealYm, RtmsApiResult.Status.NO_RESULT, 0, 0, 0, 0, message);
        }

        private static CollectionResult failed(RtmsSourceType sourceType, String lawdCd, String dealYm, String message) {
            return new CollectionResult(sourceType, lawdCd, dealYm, RtmsApiResult.Status.FAILED, 0, 0, 0, 1, message);
        }
    }

    public record CollectionAllResult(
            RtmsSourceType sourceType,
            String lawdCd,
            String dealYm,
            RtmsApiResult.Status status,
            int fetchedCount,
            int savedCount,
            int duplicateCount,
            int failedCount,
            int collectedPageCount,
            Integer totalCount,
            String message
    ) {

        private static CollectionAllResult noResult(
                RtmsSourceType sourceType,
                String lawdCd,
                String dealYm,
                Integer totalCount,
                String message) {
            return new CollectionAllResult(
                    sourceType, lawdCd, dealYm, RtmsApiResult.Status.NO_RESULT, 0, 0, 0, 0, 0, totalCount, message);
        }

        private static CollectionAllResult failed(
                RtmsSourceType sourceType,
                String lawdCd,
                String dealYm,
                int fetchedCount,
                int savedCount,
                int duplicateCount,
                int collectedPageCount,
                Integer totalCount,
                String message) {
            return new CollectionAllResult(
                    sourceType,
                    lawdCd,
                    dealYm,
                    RtmsApiResult.Status.FAILED,
                    fetchedCount,
                    savedCount,
                    duplicateCount,
                    1,
                    collectedPageCount,
                    totalCount,
                    message
            );
        }
    }
}
