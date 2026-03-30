package com.dev.IbioScience.dto.seller.order;

import java.time.LocalDateTime;

import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.enums.order.OrderStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SellerOrderListItemDto {

    private Long id;
    private String orderNo;

    private String ordererName;
    private String contact;
    private String email;

    private String companyName;
    private String shopName;

    private DealerType dealerType;
    private String dealerTypeLabel;

    private LocalDateTime orderedAt;

    private OrderStatus status;
    private String statusLabel;
}