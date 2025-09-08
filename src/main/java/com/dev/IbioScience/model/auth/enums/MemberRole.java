package com.dev.IbioScience.model.auth.enums;

/** 멤버 권한(보안 ROLE) */
public enum MemberRole {
    ROOT,          // 루트 (최초 개발자 등록)
    MASTER,        // 최고권한자(삭제 불가)
    OPERATOR,      // 운영관리자
    ADMIN,         // 일반관리자
    USER,          // 일반회원(소비자)
    BUYER_DEALER,  // 구매딜러(개인/기업)
    SELLER_DEALER  // 입점/판매딜러(기업) - 구매 권한 포함
}