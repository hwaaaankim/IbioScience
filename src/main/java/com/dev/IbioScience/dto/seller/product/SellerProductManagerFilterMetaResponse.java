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
public class SellerProductManagerFilterMetaResponse {

    @Builder.Default
    private List<SellerProductManagerCategoryLargeResponse> largeCategories = new ArrayList<>();
}