package com.example.demo.announcement.dto;

public record MarketComparisonResponse(
        Long publicDepositAmount,
        Long publicMonthlyRentAmount,
        Long marketAverageDepositAmount,
        Long marketAverageMonthlyRentAmount,
        String comparisonSummary
) {
}
