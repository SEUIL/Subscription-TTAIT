package com.ttait.subscription.admin.dto;

public record MarketRtmsSnapshotBatchResponse(
        String status,
        RtmsCollectionAllResponse collection,
        MarketSnapshotAggregateResponse snapshot,
        boolean snapshotAggregated,
        String message
) {
}
