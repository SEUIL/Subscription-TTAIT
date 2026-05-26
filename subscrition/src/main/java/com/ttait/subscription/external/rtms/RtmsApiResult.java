package com.ttait.subscription.external.rtms;

import java.util.List;

public record RtmsApiResult(
        Status status,
        List<RtmsTransactionItem> items,
        String message,
        Integer totalCount,
        Integer pageNo,
        Integer numOfRows
) {

    public enum Status {
        SUCCESS,
        NO_RESULT,
        FAILED
    }

    public static RtmsApiResult success(List<RtmsTransactionItem> items) {
        return success(items, null, null, null);
    }

    public static RtmsApiResult success(List<RtmsTransactionItem> items,
                                        Integer totalCount,
                                        Integer pageNo,
                                        Integer numOfRows) {
        return new RtmsApiResult(Status.SUCCESS, List.copyOf(items), null, totalCount, pageNo, numOfRows);
    }

    public static RtmsApiResult noResult(String message) {
        return noResult(message, null, null, null);
    }

    public static RtmsApiResult noResult(String message,
                                         Integer totalCount,
                                         Integer pageNo,
                                         Integer numOfRows) {
        return new RtmsApiResult(Status.NO_RESULT, List.of(), message, totalCount, pageNo, numOfRows);
    }

    public static RtmsApiResult failed(String message) {
        return new RtmsApiResult(Status.FAILED, List.of(), message, null, null, null);
    }
}
