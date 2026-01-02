package com.dev.IbioScience.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateItemDTO {

    private Long productId;

    private Long optionGroupId; // nullable
    private Long optionId;      // nullable

    private String productName;
    private String productImageUrl;

    private String optionGroupName;
    private String optionName;
    private String optionCode;
    private String unit;

    private Long unitPrice;
    private Integer quantity;
    private Long linePrice;
}