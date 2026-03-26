package com.dev.IbioScience.enums.product.dealer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SellerProductManagerDateType {

    CREATED_AT("등록일기준"),
    UPDATED_AT("수정일기준");

    private final String label;
}