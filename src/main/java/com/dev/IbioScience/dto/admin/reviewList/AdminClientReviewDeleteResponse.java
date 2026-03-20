package com.dev.IbioScience.dto.admin.reviewList;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminClientReviewDeleteResponse {

    private final boolean success;
    private final int deletedCount;
    private final String message;
}