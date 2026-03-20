package com.dev.IbioScience.dto.admin.reviewList;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminClientReviewListItem {

    private final Long reviewId;
    private final Long productId;
    private final String authorName;
    private final String thumbnailUrl;
    private final Long imageCount;
    private final Integer rating;
    private final String createdAtText;
    private final String content;
}