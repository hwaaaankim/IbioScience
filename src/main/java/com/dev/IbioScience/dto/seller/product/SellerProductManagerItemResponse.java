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
public class SellerProductManagerItemResponse {

    private Long dealerProductId;
    private String code;

    @Builder.Default
    private List<String> categoryPaths = new ArrayList<>();

    private String mainImageUrl;
    private String name;
    private Integer consumerPrice;
    private Integer salePrice;

    private String saleStatus;
    private String saleStatusLabel;

    private String displayStatus;
    private String displayStatusLabel;

    private String detailUrl;
}