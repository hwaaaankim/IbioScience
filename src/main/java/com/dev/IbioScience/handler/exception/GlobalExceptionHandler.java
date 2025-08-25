package com.dev.IbioScience.handler.exception;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<Map<String, Object>> json(HttpStatus status, String message, HttpServletRequest req) {
        Map<String, Object> body = Map.of(
            "success", false,
            "status", status.value(),
            "error", status.getReasonPhrase(),
            "message", Optional.ofNullable(message).orElse(""),
            "path", req != null ? req.getRequestURI() : "",
            "timestamp", LocalDateTime.now().toString()
        );
        return ResponseEntity
            .status(status)
            .contentType(MediaType.APPLICATION_JSON) // ★ JSON으로 명시 고정
            .body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        return json(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        return json(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoSuchElementException ex, HttpServletRequest req) {
        return json(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex, HttpServletRequest req) {
        // 필요시 서버 로그
        // ex.printStackTrace();
        return json(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", req);
    }
    
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        // 스택 로그 남기지 않음 (필요하면 log.debug 정도)
        return json(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다.", req);
    }
}
