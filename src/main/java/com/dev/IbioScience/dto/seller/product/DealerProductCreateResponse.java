package com.dev.IbioScience.dto.seller.product;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DealerProductCreateResponse {

    private Long dealerProductId;
    private String message;
}