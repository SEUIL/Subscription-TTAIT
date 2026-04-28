package com.ttait.subscription.common.exception;

import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApi(ApiException e) {
        return ResponseEntity.status(e.getStatus())
                .body(Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("validation failed");
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "message", message));
    }

    // 미처리 예외 catch-all — 403 대신 실제 에러 메시지를 반환하기 위해 추가
    // (STATELESS 환경에서 미처리 예외가 /error로 포워딩되면 Spring Security가 403을 반환하는 문제 방지)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpected(Exception e) {
        return ResponseEntity.internalServerError()
                .body(Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "message", "서버 내부 오류: " + e.getMessage()));
    }
}
