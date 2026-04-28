package com.ttait.subscription.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Size(max = 50) String loginId,           // 로그인 ID (최대 50자)
        @NotBlank @Size(min = 8, max = 100) String password, // 비밀번호 (8~100자)
        @NotBlank @Size(max = 30) String phone,              // 휴대폰 번호 (최대 30자)
        @NotBlank @Email @Size(max = 100) String email       // 이메일 (최대 100자)
) {
}
