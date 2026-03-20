package com.dev.IbioScience.repository.auth.review;

import java.time.LocalDateTime;

public interface AdminClientReviewListRowProjection {

    Long getReviewId();

    Long getProductId();

    Long getMemberId();

    String getAuthorName();

    Integer getRating();

    LocalDateTime getCreatedAt();

    String getFirstImageUrl();

    Long getImageCount();

    String getContent();
}