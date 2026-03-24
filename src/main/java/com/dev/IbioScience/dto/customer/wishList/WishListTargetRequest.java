package com.dev.IbioScience.dto.customer.wishList;

import com.dev.IbioScience.enums.product.dealer.WishListProductType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WishListTargetRequest {

    private WishListProductType productType;
    private Long targetId;
}