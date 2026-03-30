package com.dev.IbioScience.dto.seller.product.review;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteDealerProductReviewsRequest {

    private List<Long> reviewIds = new ArrayList<>();
}