package com.ttait.subscription.auth.dto;

public record AuthResponse(
        Long userId,        // 사용자 PK
        String loginId,     // 로그인 ID
        String accessToken  // JWT 액세스 토큰
) {
}
