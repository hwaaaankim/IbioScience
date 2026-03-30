package com.dev.IbioScience.dto.seller.order;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SellerOrderDetailItemDto {

    private Long orderItemId;
    private Long dealerProductId;

    private String productName;
    private String productImageUrl;

    private String optionGroupName;
    private String optionName;
    private String optionCode;
    private String unitText;

    private Long unitPrice;
    private Integer quantity;
    private Long linePrice;

    private String productDetailUrl;
}