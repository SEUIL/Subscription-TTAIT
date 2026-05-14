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
        Boolean alreadyImported,
        Boolean canParse,
        String dedupeStatus
) {
}
