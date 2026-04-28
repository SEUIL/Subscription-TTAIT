package com.ttait.subscription.announcement.domain;

public enum ParseReviewStatus {
    PENDING,   // 검수 대기 중
    APPROVED,  // 검수 승인 (AI 파싱 결과 확정)
    CORRECTED, // 관리자가 값을 수정하여 확정
    REJECTED,  // 검수 거부 (폐기)
    RE_IMPORT  // 재수집 요청 (LH API + PDF 재파싱)
}
