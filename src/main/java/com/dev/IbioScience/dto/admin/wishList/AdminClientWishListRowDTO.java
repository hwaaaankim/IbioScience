package com.dev.IbioScience.dto.admin.wishList;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminClientWishListRowDTO {

    private Long wishListItemId;
    private Long productId;

    private String productName;
    private String categoryPath;
    private String brandName;
    private String mainImageUrl;
}