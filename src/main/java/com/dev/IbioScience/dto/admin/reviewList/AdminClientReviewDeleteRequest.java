package com.dev.IbioScience.dto.admin.reviewList;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminClientReviewDeleteRequest {

    private List<Long> reviewIds;
}