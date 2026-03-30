package com.dev.IbioScience.dto.seller.product.review;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DealerProductReviewAdminSearchCondition {

    private int page = 0;
    private int size = 10;

    private LocalDate fromDate;
    private LocalDate toDate;

    /**
     * reviewerId / rating / createdAt
     */
    private String sortField = "createdAt";

    /**
     * asc / desc
     */
    private String sortDir = "desc";
}