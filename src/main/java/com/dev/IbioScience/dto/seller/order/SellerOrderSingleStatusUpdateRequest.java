package com.dev.IbioScience.dto.seller.order;

import com.dev.IbioScience.enums.order.OrderStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerOrderSingleStatusUpdateRequest {

    @NotNull
    private OrderStatus status;
}