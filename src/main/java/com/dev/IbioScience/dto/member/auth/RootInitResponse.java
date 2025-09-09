package com.dev.IbioScience.dto.member.auth;

/** 루트 생성 결과 응답 */
public record RootInitResponse(
        boolean created,     // true: 새로 생성, false: 이미 존재
        Long memberId,
        String loginId,
        String role,
        String message
) {}