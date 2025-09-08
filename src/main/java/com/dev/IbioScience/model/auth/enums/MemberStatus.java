package com.dev.IbioScience.model.auth.enums;

/** 멤버 상태 */
public enum MemberStatus {
    ACTIVE,     // 정상
    PENDING,    // 대기(미인증/검수중)
    SUSPENDED,  // 중지(일시정지)
    DORMANT,    // 휴면
    WITHDRAWN,  // 탈퇴
    DELETED     // 삭제(논리 삭제 시)
}