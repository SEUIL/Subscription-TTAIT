package com.ttait.subscription.config;

import com.ttait.subscription.auth.jwt.JwtAuthenticationFilter;
import com.ttait.subscription.auth.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        http
                // REST API는 CSRF 토큰 불필요 — 세션 기반 인증을 사용하지 않기 때문
                .csrf(csrf -> csrf.disable())
                // JWT 기반 무상태 인증 — 서버가 세션을 유지하지 않음
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 로그인·회원가입은 인증 없이 접근 가능
                        .requestMatchers("/api/auth/**").permitAll()
                        // Spring Boot 기본 에러 엔드포인트 — STATELESS 세션 정책에서 에러 디스패치 시 인증 컨텍스트가 끊기므로 공개 설정 필요
                        .requestMatchers("/error").permitAll()
                        // Swagger UI는 개발 편의상 공개 (운영 배포 시 별도 제한 필요)
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**").permitAll()
                        // 공고 목록/상세 조회는 비로그인 사용자도 가능 (GET만 허용)
                        .requestMatchers(HttpMethod.GET, "/api/announcements/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/filters/**").permitAll()
                        // 관리자 전용 API — ADMIN 권한 없으면 403 반환
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 나머지 모든 요청은 로그인 필수
                        .anyRequest().authenticated())
                // JWT 검증 필터를 Spring Security 기본 인증 필터 앞에 배치
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
