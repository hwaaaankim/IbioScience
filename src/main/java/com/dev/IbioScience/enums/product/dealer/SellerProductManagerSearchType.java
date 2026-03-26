package com.dev.IbioScience.enums.product.dealer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SellerProductManagerSearchType {

    KEYWORD("키워드검색"),
    PRODUCT_NAME("제품명검색");

    private final String label;
}