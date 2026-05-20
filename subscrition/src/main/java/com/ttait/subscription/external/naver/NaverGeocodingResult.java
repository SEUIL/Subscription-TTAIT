package com.ttait.subscription.external.naver;

import java.math.BigDecimal;

public record NaverGeocodingResult(
        Status status,
        BigDecimal latitude,
        BigDecimal longitude,
        String message
) {

    public enum Status {
        SUCCESS,
        NO_RESULT,
        FAILED
    }

    public static NaverGeocodingResult success(BigDecimal latitude, BigDecimal longitude) {
        return new NaverGeocodingResult(Status.SUCCESS, latitude, longitude, null);
    }

    public static NaverGeocodingResult noResult(String message) {
        return new NaverGeocodingResult(Status.NO_RESULT, null, null, message);
    }

    public static NaverGeocodingResult failed(String message) {
        return new NaverGeocodingResult(Status.FAILED, null, null, message);
    }

    public boolean successful() {
        return status == Status.SUCCESS;
    }
}
