package com.dev.IbioScience.enums.product.coupon;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberCouponHistoryActionType {

    ISSUE("발급"),
    USE("사용"),
    DELETE("삭제");

    private final String label;
}