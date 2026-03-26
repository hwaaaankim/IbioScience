package com.dev.IbioScience.dto.seller.product;

import java.util.ArrayList;
import java.util.List;

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
public class SellerProductManagerCategoryLargeResponse {

    private Long id;
    private String name;

    @Builder.Default
    private List<SellerProductManagerCategoryMediumResponse> mediums = new ArrayList<>();
}