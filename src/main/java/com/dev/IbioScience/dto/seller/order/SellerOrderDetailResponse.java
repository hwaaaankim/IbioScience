package com.dev.IbioScience.dto.seller.order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.enums.order.OrderStatus;
import com.dev.IbioScience.enums.order.PaymentMethod;
import com.dev.IbioScience.enums.order.ShippingMethod;
import com.dev.IbioScience.enums.order.ShippingPayType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SellerOrderDetailResponse {

    private Long orderId;
    private String orderNo;

    private OrderStatus status;
    private String statusLabel;

    private LocalDateTime orderedAt;
    private LocalDateTime paidAt;

    private String ordererName;
    private String ordererUsername;
    private String contact;
    private String email;

    private DealerType dealerType;
    private String dealerTypeLabel;

    private String companyName;
    private String shopName;

    private String receiverName;

    private String hp1;
    private String hp2;
    private String hp3;

    private String tel1;
    private String tel2;
    private String tel3;

    private String postcode;
    private String roadAddress;
    private String detailAddress;
    private String shippingMemo;

    private PaymentMethod paymentMethod;
    private String paymentMethodLabel;

    private ShippingMethod shippingMethod;
    private String shippingMethodLabel;

    private ShippingPayType shippingPayType;
    private String shippingPayTypeLabel;

    private int visibleItemCount;
    private long visibleItemSumPrice;

    @Builder.Default
    private List<SellerOrderDetailItemDto> items = new ArrayList<>();
}