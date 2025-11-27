package com.dev.IbioScience.dto.page.productList;

import lombok.Data;

@Data
public class ProductRatingSummaryDto {
    private Long productId;
    private Double averageRating;
    private Long reviewCount;

    public ProductRatingSummaryDto(Long productId, Double averageRating, Long reviewCount) {
        this.productId = productId;
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }
}