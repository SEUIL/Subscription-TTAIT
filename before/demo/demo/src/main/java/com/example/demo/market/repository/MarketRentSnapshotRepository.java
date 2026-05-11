package com.example.demo.market.repository;

import com.example.demo.market.domain.MarketRentSnapshot;
import com.example.demo.market.domain.MarketSourceType;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketRentSnapshotRepository extends JpaRepository<MarketRentSnapshot, Long> {
    List<MarketRentSnapshot> findBySourceTypeAndLawdCodeAndExclusiveAreaBetween(
            MarketSourceType sourceType,
            String lawdCode,
            BigDecimal minArea,
            BigDecimal maxArea
    );

    List<MarketRentSnapshot> findBySourceTypeAndLawdCode(MarketSourceType sourceType, String lawdCode);
}
