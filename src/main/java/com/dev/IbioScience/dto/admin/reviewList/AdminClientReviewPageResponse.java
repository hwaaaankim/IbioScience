package com.dev.IbioScience.dto.admin.reviewList;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminClientReviewPageResponse {

    private final List<AdminClientReviewListItem> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final int numberOfElements;
    private final boolean first;
    private final boolean last;
    private final boolean empty;
}