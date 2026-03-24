package com.dev.IbioScience.enums.front.dealerProductList;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DealerProductListOptionRowDto {

    private final Long optionGroupId;
    private final String optionGroupName;

    private final Long optionId;
    private final String optionName;
    private final String optionCode;

    private final String catNo;
    private final String unit;

    private final Integer unitPrice;
    private final String unitPriceText;
}