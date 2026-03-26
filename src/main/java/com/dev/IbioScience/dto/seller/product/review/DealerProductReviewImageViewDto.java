package com.dev.IbioScience.dto.seller.product.review;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DealerProductReviewImageViewDto {

    private Long id;
    private String url;
    private String originalFilename;
}