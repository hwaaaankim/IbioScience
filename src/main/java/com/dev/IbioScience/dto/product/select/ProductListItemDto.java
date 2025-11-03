package com.dev.IbioScience.dto.product.select;

import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.ProductNewState;
import com.dev.IbioScience.enums.product.SaleStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductListItemDto {
    private Long id;
    private String name;
    private String code;
    private Integer salePrice;
    private Integer consumerPrice;
    private String brandName;
    private String mainImageUrl;
    private DisplayStatus displayStatus;
    private SaleStatus saleStatus;
    private ProductNewState newState;
    private Double averageRating;
    private Long reviewCount;
}