package com.ttait.subscription.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String loginId,  // 로그인 ID
        @NotBlank String password  // 비밀번호
) {
}
