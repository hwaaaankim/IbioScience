package com.dev.IbioScience.handler.exception;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice(annotations = ResponseBody.class)
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
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(Exception ex, HttpServletRequest req) {
        return json(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.", req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        return json(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        return json(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoSuchElementException ex, HttpServletRequest req) {
        return json(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        return json(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다.", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex, HttpServletRequest req) {
        return json(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", req);
    }
}

