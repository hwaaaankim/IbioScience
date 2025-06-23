package com.dev.IbioScience.handler.exception;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class) // ★★ 꼭 추가! ★★
    public ResponseEntity<?> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST) // 400 BAD_REQUEST로 맞추세요!
            .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(Exception ex) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("message", "서버 내부 오류가 발생했습니다."));
    }
}