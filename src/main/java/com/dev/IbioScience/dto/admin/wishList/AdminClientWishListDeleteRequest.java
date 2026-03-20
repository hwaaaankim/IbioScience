package com.dev.IbioScience.dto.admin.wishList;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminClientWishListDeleteRequest {

    private List<Long> wishListItemIds;
}