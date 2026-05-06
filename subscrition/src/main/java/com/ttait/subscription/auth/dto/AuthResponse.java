package com.ttait.subscription.auth.dto;

public record AuthResponse(
        Long userId,
        String loginId,
        String accessToken,
        boolean profileCompleted,
        String role
) {
}
