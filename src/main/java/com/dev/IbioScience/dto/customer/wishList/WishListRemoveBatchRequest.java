package com.dev.IbioScience.dto.customer.wishList;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WishListRemoveBatchRequest {

    private List<WishListTargetRequest> items = new ArrayList<>();
}