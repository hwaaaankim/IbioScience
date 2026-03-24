package com.dev.IbioScience.enums.front.dealerProductList;
public enum DealerProductListSort {

    NAME_ASC,
    NAME_DESC,
    PRICE_ASC,
    PRICE_DESC;

    public static DealerProductListSort from(String value) {
        if (value == null || value.isBlank()) {
            return NAME_ASC;
        }

        for (DealerProductListSort sort : values()) {
            if (sort.name().equalsIgnoreCase(value)) {
                return sort;
            }
        }

        return NAME_ASC;
    }
}