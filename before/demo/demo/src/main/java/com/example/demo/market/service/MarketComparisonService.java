package com.example.demo.market.service;

import com.example.demo.announcement.dto.MarketComparisonResponse;
import com.example.demo.announcement.repository.query.AnnouncementDetailRow;
import com.example.demo.market.domain.LawdCodeMapping;
import com.example.demo.market.domain.MarketRentSnapshot;
import com.example.demo.market.domain.MarketSourceType;
import com.example.demo.market.repository.LawdCodeMappingRepository;
import com.example.demo.market.repository.MarketRentSnapshotRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MarketComparisonService {

    private static final String INSUFFICIENT_DATA_MESSAGE = "비교 가능한 주변 시세 데이터가 부족함";

    private final LawdCodeMappingRepository lawdCodeMappingRepository;
    private final MarketRentSnapshotRepository marketRentSnapshotRepository;

    public MarketComparisonService(LawdCodeMappingRepository lawdCodeMappingRepository,
                                   MarketRentSnapshotRepository marketRentSnapshotRepository) {
        this.lawdCodeMappingRepository = lawdCodeMappingRepository;
        this.marketRentSnapshotRepository = marketRentSnapshotRepository;
    }

    public MarketComparisonResponse compare(AnnouncementDetailRow row) {
        if (row.depositAmount() == null && row.monthlyRentAmount() == null) {
            return null;
        }

        MarketSourceType sourceType = resolveSourceType(row.houseType());
        if (sourceType == null) {
            return null;
        }

        Optional<LawdCodeMapping> mapping = lawdCodeMappingRepository.findBySidoNameAndSigunguNameAndActiveTrue(
                extractSido(row.fullAddress()),
                extractSigungu(row.fullAddress())
        );
        if (mapping.isEmpty()) {
            return emptyComparison(row, INSUFFICIENT_DATA_MESSAGE);
        }

        List<MarketRentSnapshot> snapshots = findSnapshots(row, sourceType, mapping.get().getSigunguCode5());
        if (snapshots.isEmpty()) {
            return emptyComparison(row, INSUFFICIENT_DATA_MESSAGE);
        }

        long avgDeposit = Math.round(snapshots.stream().mapToLong(MarketRentSnapshot::getDepositAmount).average().orElse(0));
        long avgMonthlyRent = Math.round(snapshots.stream().mapToLong(MarketRentSnapshot::getMonthlyRentAmount).average().orElse(0));

        return new MarketComparisonResponse(
                row.depositAmount(),
                row.monthlyRentAmount(),
                avgDeposit,
                avgMonthlyRent,
                buildSummary(row.depositAmount(), row.monthlyRentAmount(), avgDeposit, avgMonthlyRent)
        );
    }

    private List<MarketRentSnapshot> findSnapshots(AnnouncementDetailRow row, MarketSourceType sourceType, String lawdCode) {
        if (row.exclusiveAreaValue() == null) {
            return marketRentSnapshotRepository.findBySourceTypeAndLawdCode(sourceType, lawdCode);
        }

        BigDecimal area = row.exclusiveAreaValue();
        return marketRentSnapshotRepository.findBySourceTypeAndLawdCodeAndExclusiveAreaBetween(
                sourceType,
                lawdCode,
                area.multiply(new BigDecimal("0.8")),
                area.multiply(new BigDecimal("1.2"))
        );
    }

    private MarketComparisonResponse emptyComparison(AnnouncementDetailRow row, String summary) {
        return new MarketComparisonResponse(
                row.depositAmount(),
                row.monthlyRentAmount(),
                null,
                null,
                summary
        );
    }

    private String buildSummary(Long deposit, Long rent, long avgDeposit, long avgRent) {
        if (rent != null && avgRent > 0 && rent <= avgRent * 0.8) {
            return "주변 시세 대비 월세 부담이 낮음";
        }
        if (deposit != null && avgDeposit > 0 && deposit <= avgDeposit * 0.8) {
            return "주변 시세 대비 보증금 부담이 낮음";
        }
        return "주변 시세와 유사";
    }

    private MarketSourceType resolveSourceType(String houseType) {
        if (houseType == null || houseType.isBlank()) {
            return null;
        }
        if ("아파트".equals(houseType)) {
            return MarketSourceType.APT_RENT;
        }
        if ("다가구".equals(houseType) || "다세대/연립".equals(houseType)) {
            return MarketSourceType.ROW_HOUSE_RENT;
        }
        return null;
    }

    private String extractSido(String fullAddress) {
        if (fullAddress == null || fullAddress.isBlank()) {
            return null;
        }
        String[] parts = fullAddress.trim().split("\\s+");
        return parts.length >= 1 ? parts[0] : null;
    }

    private String extractSigungu(String fullAddress) {
        if (fullAddress == null || fullAddress.isBlank()) {
            return null;
        }
        String[] parts = fullAddress.trim().split("\\s+");
        return parts.length >= 2 ? parts[1] : null;
    }
}
