package com.dev.IbioScience.dto.admin.reviewList;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminClientReviewSearchCondition {

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 10;

    private LocalDate fromDate;
    private LocalDate toDate;
}