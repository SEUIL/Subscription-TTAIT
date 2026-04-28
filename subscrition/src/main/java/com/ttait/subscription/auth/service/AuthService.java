package com.ttait.subscription.auth.service;

import com.ttait.subscription.auth.dto.AuthResponse;
import com.ttait.subscription.auth.dto.LoginRequest;
import com.ttait.subscription.auth.dto.SignupRequest;
import com.ttait.subscription.auth.jwt.JwtTokenProvider;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.user.domain.User;
import com.ttait.subscription.user.domain.enums.Role;
import com.ttait.subscription.user.domain.enums.UserStatus;
import com.ttait.subscription.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new ApiException(HttpStatus.CONFLICT, "loginId already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "email already exists");
        }
        User user = userRepository.save(User.builder()
                .loginId(request.loginId())
                .passwordHash(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .email(request.email())
                .status(UserStatus.ACTIVE)
                .role(Role.USER)
                .build());
        String token = jwtTokenProvider.generateToken(user.getId(), user.getLoginId(), user.getRole());
        return new AuthResponse(user.getId(), user.getLoginId(), token);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByLoginIdAndDeletedFalse(request.loginId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid credentials");
        }
        String token = jwtTokenProvider.generateToken(user.getId(), user.getLoginId(), user.getRole());
        return new AuthResponse(user.getId(), user.getLoginId(), token);
    }
}
