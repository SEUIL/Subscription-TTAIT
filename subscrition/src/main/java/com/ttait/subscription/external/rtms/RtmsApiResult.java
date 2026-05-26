package com.ttait.subscription.external.rtms;

import java.util.List;

public record RtmsApiResult(
        Status status,
        List<RtmsTransactionItem> items,
        String message
) {

    public enum Status {
        SUCCESS,
        NO_RESULT,
        FAILED
    }

    public static RtmsApiResult success(List<RtmsTransactionItem> items) {
        return new RtmsApiResult(Status.SUCCESS, List.copyOf(items), null);
    }

    public static RtmsApiResult noResult(String message) {
        return new RtmsApiResult(Status.NO_RESULT, List.of(), message);
    }

    public static RtmsApiResult failed(String message) {
        return new RtmsApiResult(Status.FAILED, List.of(), message);
    }
}
