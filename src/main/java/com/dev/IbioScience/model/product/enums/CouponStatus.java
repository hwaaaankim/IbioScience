package com.dev.IbioScience.model.product.enums;

public enum CouponStatus {
    ISSUED("발급됨"),
    USED("사용됨"),
    EXPIRED("만료됨");

    private final String label;

    CouponStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}