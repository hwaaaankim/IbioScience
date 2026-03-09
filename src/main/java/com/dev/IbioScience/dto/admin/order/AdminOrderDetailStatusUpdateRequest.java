package com.dev.IbioScience.dto.admin.order;

import com.dev.IbioScience.enums.order.OrderStatus;
import com.dev.IbioScience.enums.order.PaymentMethod;
import com.dev.IbioScience.enums.order.ShippingMethod;
import com.dev.IbioScience.enums.order.ShippingPayType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminOrderDetailStatusUpdateRequest {

    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private ShippingMethod shippingMethod;
    private ShippingPayType shippingPayType;
}