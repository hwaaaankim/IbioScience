package com.dev.IbioScience.dto.seller.order;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.enums.order.OrderStatus;
import com.dev.IbioScience.enums.order.PaymentMethod;
import com.dev.IbioScience.enums.order.ShippingMethod;
import com.dev.IbioScience.enums.order.ShippingPayType;
import com.dev.IbioScience.enums.order.dealer.SellerOrderKeywordType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerOrderSearchCondition {

    private Integer page = 0;
    private Integer size = 10;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    private SellerOrderKeywordType keywordType;
    private String keyword;

    private List<DealerType> dealerTypes = new ArrayList<>();
    private List<OrderStatus> statuses = new ArrayList<>();
    private List<PaymentMethod> paymentMethods = new ArrayList<>();
    private List<ShippingMethod> shippingMethods = new ArrayList<>();
    private List<ShippingPayType> shippingPayTypes = new ArrayList<>();

    private String sortField = "orderedAt";
    private String sortDir = "desc";
}