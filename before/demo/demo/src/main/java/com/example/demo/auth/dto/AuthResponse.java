package com.example.demo.auth.dto;

public record AuthResponse(
        Long userId,
        String loginId,
        String accessToken
) {
}
