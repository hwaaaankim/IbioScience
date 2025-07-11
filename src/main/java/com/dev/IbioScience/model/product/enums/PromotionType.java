package com.dev.IbioScience.model.product.enums;

//할인/증정 정책 타입 - 할인인지 증정인지
public enum PromotionType {
    DISCOUNT("할인"),
    GIFT("증정"),
    ONE_PLUS_ONE("1+1"),
    COUPON("쿠폰발행");

    private final String label;
    PromotionType(String label) { this.label = label; }
    public String getLabel() { return label; }
}