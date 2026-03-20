package com.dev.IbioScience.dto.admin.wishList;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminClientWishListDeleteResponse {

    private int deletedCount;
}