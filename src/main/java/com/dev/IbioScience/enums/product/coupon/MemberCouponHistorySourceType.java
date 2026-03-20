package com.dev.IbioScience.enums.product.coupon;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberCouponHistorySourceType {

    ADMIN("관리자"),
    ORDER_PROMOTION("주문/프로모션"),
    ORDER_USE("주문사용");

    private final String label;
}