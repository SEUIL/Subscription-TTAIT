package com.ttait.subscription.market.repository;

import com.ttait.subscription.market.domain.MarketSourceType;
import com.ttait.subscription.market.domain.MarketTransactionRaw;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketTransactionRawRepository extends JpaRepository<MarketTransactionRaw, Long> {

    Optional<MarketTransactionRaw> findByRawPayloadHash(String rawPayloadHash);

    boolean existsByRawPayloadHash(String rawPayloadHash);

    List<MarketTransactionRaw> findBySourceTypeAndLawdCdAndDealYmBetweenAndExclusiveAreaBetween(
            MarketSourceType sourceType,
            String lawdCd,
            String dealYmFrom,
            String dealYmTo,
            BigDecimal areaMin,
            BigDecimal areaMax);

    long countBySourceTypeAndLawdCdAndDealYmBetweenAndExclusiveAreaBetween(
            MarketSourceType sourceType,
            String lawdCd,
            String dealYmFrom,
            String dealYmTo,
            BigDecimal areaMin,
            BigDecimal areaMax);
}
