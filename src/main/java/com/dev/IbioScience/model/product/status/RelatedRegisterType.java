package com.dev.IbioScience.model.product.status;

public enum RelatedRegisterType {
    PRODUCT("상호등록"),
    GENERAL("일방등록");

    private final String label;
    RelatedRegisterType(String label) { this.label = label; }
    public String getLabel() { return label; }
}