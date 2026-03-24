package com.dev.IbioScience.enums.product.dealer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductSourceType {

    COMPANY("우리회사제품"),
    DEALER("딜러제품");

    private final String label;
}