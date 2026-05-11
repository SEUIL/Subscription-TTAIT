package com.example.demo.auth.security;

public record AuthenticatedUser(
        Long userId,
        String loginId
) {
}
