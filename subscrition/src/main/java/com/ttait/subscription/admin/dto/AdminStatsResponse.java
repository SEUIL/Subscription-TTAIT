package com.ttait.subscription.admin.dto;

// 관리자 검수 통계 대시보드 응답 DTO
public record AdminStatsResponse(

        long pending,             // 검수 대기 건수
        long approved,            // 승인 완료 건수
        long corrected,           // 수정 완료 건수
        long rejected,            // 거절 건수
        long reImport,            // 재수집 대기 건수

        long totalAnnouncements,  // 전체 공고 수 (삭제·병합 제외)
        long processedToday       // 오늘 처리된 검수 건수
) {}
