package com.ttait.subscription.external.rtms;

public enum RtmsSourceType {
    APT_RENT("/OpenAPI_ToolInstallPackage/service/rest/RTMSOBJSvc/getRTMSDataSvcAptRent"),
    APT_TRADE("/OpenAPI_ToolInstallPackage/service/rest/RTMSOBJSvc/getRTMSDataSvcAptTrade"),
    ROW_HOUSE_RENT("/OpenAPI_ToolInstallPackage/service/rest/RTMSOBJSvc/getRTMSDataSvcRHTradeRent"),
    ROW_HOUSE_TRADE("/OpenAPI_ToolInstallPackage/service/rest/RTMSOBJSvc/getRTMSDataSvcRHTrade"),
    OFFICETEL_RENT("/OpenAPI_ToolInstallPackage/service/rest/RTMSOBJSvc/getRTMSDataSvcOffiRent"),
    OFFICETEL_TRADE("/OpenAPI_ToolInstallPackage/service/rest/RTMSOBJSvc/getRTMSDataSvcOffiTrade");

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
