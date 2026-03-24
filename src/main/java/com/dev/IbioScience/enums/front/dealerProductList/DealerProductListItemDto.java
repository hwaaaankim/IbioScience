package com.dev.IbioScience.enums.front.dealerProductList;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DealerProductListItemDto {

    private final Long productId;
    private final String productType;

    private final String name;
    private final String code;
    private final String shortDescription;

    private final String brandName;
    private final String imageUrl;
    private final String detailUrl;

    private final Integer salePrice;
    private final String displayPriceText;

    private final boolean hasOptions;
    private final boolean canDirectCart;

    private final List<DealerProductListOptionRowDto> optionRows;
}