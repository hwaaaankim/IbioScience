package com.dev.IbioScience.dto.view;

/** 뷰/템플릿 분기용 회원 유형 */
public enum UiMemberType {
    PERSONAL_USER,          // 개인 일반회원
    PERSONAL_BUYER_DEALER,  // 개인 구매딜러
    COMPANY_USER,           // 법인 일반
    COMPANY_BUYER_DEALER,   // 법인 구매딜러
    COMPANY_SELLER_DEALER,  // 법인 판매+구매딜러
    STAFF                   // 우리 회사쪽 직원
}