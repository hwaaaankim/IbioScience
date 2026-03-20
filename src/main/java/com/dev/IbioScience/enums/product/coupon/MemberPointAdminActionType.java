package com.dev.IbioScience.enums.product.coupon;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberPointAdminActionType {

    GRANT("부여"),
    DEDUCT("차감");

    private final String label;
}