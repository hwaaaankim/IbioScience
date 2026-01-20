package com.dev.IbioScience.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 모든 API 공통 응답 포맷
 * - code: 200/404/415 등 (업무 코드 포함 가능)
 * - success: true/false
 * - message: 메시지
 * - data: 실제 응답 데이터(기존 bankda 응답 구조도 여기 안에 그대로 유지)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommonAPIResponse<T> {

    private int code;
    private boolean success;
    private String message;
    private T data;

    public static <T> CommonAPIResponse<T> ok(T data) {
        return CommonAPIResponse.<T>builder()
                .code(200)
                .success(true)
                .message("OK")
                .data(data)
                .build();
    }

    public static <T> CommonAPIResponse<T> ok(String message, T data) {
        return CommonAPIResponse.<T>builder()
                .code(200)
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> CommonAPIResponse<T> fail(int code, String message, T data) {
        return CommonAPIResponse.<T>builder()
                .code(code)
                .success(false)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> CommonAPIResponse<T> fail(int code, String message) {
        return CommonAPIResponse.<T>builder()
                .code(code)
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}