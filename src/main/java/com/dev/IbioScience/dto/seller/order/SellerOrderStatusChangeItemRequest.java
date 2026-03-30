package com.dev.IbioScience.dto.seller.order;

import com.dev.IbioScience.enums.order.OrderStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerOrderStatusChangeItemRequest {

    @NotNull
    private Long orderId;

    @NotNull
    private OrderStatus status;
}