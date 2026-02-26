package com.dev.IbioScience.enums.auth;

public enum PricePolicy {
	GUEST,                // 비회원
	PERSONAL_NORMAL,      // 일반회원(개인) - 프로모션/일반가
	COMPANY_BUYER_GRADE,  // 기업(구매딜러) - 딜러등급가
	COMPANY_SELLER_GRADE  // 기업(판매딜러) - 딜러등급가(구매 포함)
}