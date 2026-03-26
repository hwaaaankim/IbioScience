package com.dev.IbioScience.dto.seller.product.review;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DealerProductReviewViewDto {

    private Long id;
    private Long memberId;
    private String memberDisplayName;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
    private List<DealerProductReviewImageViewDto> images;
}