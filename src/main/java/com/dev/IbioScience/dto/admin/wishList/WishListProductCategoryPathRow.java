package com.dev.IbioScience.dto.admin.wishList;

import lombok.Getter;

@Getter
public class WishListProductCategoryPathRow {

    private final Long productId;
    private final String largeName;
    private final String mediumName;
    private final String smallName;

    public WishListProductCategoryPathRow(Long productId, String largeName, String mediumName, String smallName) {
        this.productId = productId;
        this.largeName = largeName;
        this.mediumName = mediumName;
        this.smallName = smallName;
    }
}