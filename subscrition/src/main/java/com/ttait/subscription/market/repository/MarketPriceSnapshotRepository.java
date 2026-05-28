package com.ttait.subscription.market.repository;

import com.ttait.subscription.market.domain.MarketPriceSnapshot;
import com.ttait.subscription.market.domain.MarketSourceType;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketPriceSnapshotRepository extends JpaRepository<MarketPriceSnapshot, Long> {

    Optional<MarketPriceSnapshot> findBySnapshotKey(String snapshotKey);

    Optional<MarketPriceSnapshot> findFirstBySourceTypeAndLawdCdAndDealYmFromAndDealYmToAndAreaMinLessThanEqualAndAreaMaxGreaterThanEqualOrderByAggregatedAtDesc(
            MarketSourceType sourceType,
            String lawdCd,
            String dealYmFrom,
            String dealYmTo,
            BigDecimal area,
            BigDecimal sameArea);
}
