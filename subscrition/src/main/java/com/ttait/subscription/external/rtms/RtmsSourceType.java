package com.ttait.subscription.external.rtms;

public enum RtmsSourceType {
    APT_RENT("/1613000/RTMSDataSvcAptRent/getRTMSDataSvcAptRent"),
    APT_TRADE("/1613000/RTMSDataSvcAptTrade/getRTMSDataSvcAptTrade"),
    ROW_HOUSE_RENT("/1613000/RTMSDataSvcRHRent/getRTMSDataSvcRHRent"),
    ROW_HOUSE_TRADE("/1613000/RTMSDataSvcRHTrade/getRTMSDataSvcRHTrade"),
    OFFICETEL_RENT("/1613000/RTMSDataSvcOffiRent/getRTMSDataSvcOffiRent"),
    OFFICETEL_TRADE("/1613000/RTMSDataSvcOffiTrade/getRTMSDataSvcOffiTrade");

    private final String path;

    RtmsSourceType(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    public boolean rent() {
        return name().endsWith("RENT");
    }
}
