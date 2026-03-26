package com.dev.IbioScience.dto.seller.product;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerProductManagerDeleteRequest {

    private List<Long> dealerProductIds = new ArrayList<>();
}