package com.dev.IbioScience.enums.logging;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum MemberAuditAction {

    /* =========================
     * ✅ 신규 상세 이벤트(요구사항 + 직원 로그인 추가)
     * ========================= */
    PERSONAL_SIGNUP("일반회원 회원가입"),
    COMPANY_SIGNUP("기업회원 회원가입"),

    PERSONAL_WITHDRAW_REQUEST("일반회원 탈퇴요청"),

    PERSONAL_TO_COMPANY_CONVERSION("일반회원 기업전환"),

    BUYER_DEALER_TO_SELLER_REQUEST("구매 딜러회원 판매자 전환 신청"),

    PERSONAL_LOGIN("일반회원 로그인"),
    COMPANY_LOGIN("기업회원 로그인"),
    STAFF_LOGIN("직원 로그인"),

    PERSONAL_INFO_UPDATE("일반회원 정보수정"),
    COMPANY_INFO_UPDATE("기업회원 정보수정"),

    /* =========================
     * ✅ 레거시(기존 데이터 호환용)
     * ========================= */
    JOIN("가입(레거시)"),
    WITHDRAW("탈퇴(레거시)"),
    UPDATE("수정(레거시)"),
    ROLE_CHANGE("권한변경(레거시)"),

    /* 폴백 */
    OTHER("기타");

    private final String labelKr;

    MemberAuditAction(String labelKr) {
        this.labelKr = labelKr;
    }

    public String getLabelKr() {
        return labelKr;
    }

    /** Thymeleaf/JS 친화: enum.name() -> labelKr */
    public static Map<String, String> labelMap() {
        Map<String, String> m = new LinkedHashMap<>();
        Arrays.stream(values()).forEach(v -> m.put(v.name(), v.getLabelKr()));
        return Collections.unmodifiableMap(m);
    }

    /** 방어용 */
    public static MemberAuditAction safeValueOf(String name) {
        if (name == null || name.isBlank()) return OTHER;
        try {
            return MemberAuditAction.valueOf(name);
        } catch (Exception e) {
            return OTHER;
        }
    }
}