package com.ttait.subscription.market.service;

import com.ttait.subscription.external.rtms.RtmsApiResult;
import com.ttait.subscription.external.rtms.RtmsClient;
import com.ttait.subscription.external.rtms.RtmsSourceType;
import com.ttait.subscription.external.rtms.RtmsTransactionItem;
import com.ttait.subscription.external.support.CanonicalJsonHasher;
import com.ttait.subscription.market.domain.MarketSourceType;
import com.ttait.subscription.market.domain.MarketTransactionRaw;
import com.ttait.subscription.market.repository.MarketTransactionRawRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketRtmsCollectionService {

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

        int savedCount = 0;
        int duplicateCount = 0;
        for (RtmsTransactionItem item : result.items()) {
            String rawPayloadHash = rawPayloadHash(sourceType.name(), lawdCd, dealYm, item.rawPayload());
            if (rawRepository.existsByRawPayloadHash(rawPayloadHash)) {
                duplicateCount++;
                continue;
            }
            rawRepository.save(toRawTransaction(item, sourceType, lawdCd, dealYm, rawPayloadHash));
            savedCount++;
        }

        return new CollectionResult(
                sourceType,
                lawdCd,
                dealYm,
                RtmsApiResult.Status.SUCCESS,
                result.items().size(),
                savedCount,
                duplicateCount,
                0,
                result.message()
        );
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
}
