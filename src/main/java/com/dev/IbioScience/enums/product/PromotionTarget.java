package com.dev.IbioScience.enums.product;

public enum PromotionTarget {
    
	NORMAL("일반회원");

    private final String label;
    PromotionTarget(String label) { this.label = label; }
    public String getLabel() { return label; }
}