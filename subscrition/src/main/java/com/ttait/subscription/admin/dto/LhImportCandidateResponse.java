package com.ttait.subscription.admin.dto;

public record LhImportCandidateResponse(
        Long id,
        String panId,
        String title,
        String region,
        String status,
        String sourceNoticeUrl,
        String pdfUrl,
        Boolean isLandNotice,
        Boolean isCommercialNotice,
        Boolean alreadyImported,
        Boolean canParse,
        String dedupeStatus,
        String skipReason
) {

    public LhImportCandidateResponse(Long id,
                                     String panId,
                                     String title,
                                     String region,
                                     String status,
                                     String sourceNoticeUrl,
                                     String pdfUrl,
                                     Boolean isLandNotice,
                                     Boolean alreadyImported,
                                     Boolean canParse,
                                     String dedupeStatus) {
        this(id, panId, title, region, status, sourceNoticeUrl, pdfUrl, isLandNotice, false,
                alreadyImported, canParse, dedupeStatus, null);
    }
}
