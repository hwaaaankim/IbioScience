package com.dev.IbioScience.controller.seller.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.seller.order.SellerOrderBatchStatusUpdateRequest;
import com.dev.IbioScience.dto.seller.order.SellerOrderDetailResponse;
import com.dev.IbioScience.dto.seller.order.SellerOrderListResponse;
import com.dev.IbioScience.dto.seller.order.SellerOrderSearchCondition;
import com.dev.IbioScience.dto.seller.order.SellerOrderSingleStatusUpdateRequest;
import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.enums.order.OrderStatus;
import com.dev.IbioScience.enums.order.PaymentMethod;
import com.dev.IbioScience.enums.order.ShippingMethod;
import com.dev.IbioScience.enums.order.ShippingPayType;
import com.dev.IbioScience.enums.order.dealer.SellerOrderKeywordType;
import com.dev.IbioScience.service.seller.order.SellerOrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/seller/api/orders")
@RequiredArgsConstructor
@Validated
public class SellerOrderApiController {

    private final SellerOrderService sellerOrderService;

    @GetMapping
    public SellerOrderListResponse getSellerOrders(
            @AuthenticationPrincipal(expression = "member.id") Long sellerMemberId,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) java.time.LocalDate fromDate,
            @RequestParam(required = false) java.time.LocalDate toDate,
            @RequestParam(required = false) SellerOrderKeywordType keywordType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<DealerType> dealerTypes,
            @RequestParam(required = false) List<OrderStatus> statuses,
            @RequestParam(required = false) List<PaymentMethod> paymentMethods,
            @RequestParam(required = false) List<ShippingMethod> shippingMethods,
            @RequestParam(required = false) List<ShippingPayType> shippingPayTypes,
            @RequestParam(required = false, defaultValue = "orderedAt") String sortField,
            @RequestParam(required = false, defaultValue = "desc") String sortDir
    ) {
        if (sellerMemberId == null) {
            throw new AccessDeniedException("로그인 정보가 없습니다.");
        }

        SellerOrderSearchCondition condition = new SellerOrderSearchCondition();
        condition.setPage(page);
        condition.setSize(size);
        condition.setFromDate(fromDate);
        condition.setToDate(toDate);
        condition.setKeywordType(keywordType);
        condition.setKeyword(keyword);
        condition.setDealerTypes(dealerTypes);
        condition.setStatuses(statuses);
        condition.setPaymentMethods(paymentMethods);
        condition.setShippingMethods(shippingMethods);
        condition.setShippingPayTypes(shippingPayTypes);
        condition.setSortField(sortField);
        condition.setSortDir(sortDir);

        return sellerOrderService.getSellerOrders(sellerMemberId, condition);
    }

    @GetMapping("/{orderId}")
    public SellerOrderDetailResponse getSellerOrderDetail(
            @AuthenticationPrincipal(expression = "member.id") Long sellerMemberId,
            @PathVariable Long orderId
    ) {
        if (sellerMemberId == null) {
            throw new AccessDeniedException("로그인 정보가 없습니다.");
        }

        return sellerOrderService.getSellerOrderDetail(sellerMemberId, orderId);
    }

    @PatchMapping("/status")
    public Map<String, Object> updateSellerOrderStatuses(
            @AuthenticationPrincipal(expression = "member.id") Long sellerMemberId,
            @Valid @RequestBody SellerOrderBatchStatusUpdateRequest request
    ) {
        if (sellerMemberId == null) {
            throw new AccessDeniedException("로그인 정보가 없습니다.");
        }

        sellerOrderService.updateSellerOrderStatuses(sellerMemberId, request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "주문상태가 저장되었습니다.");
        return result;
    }

    @PatchMapping("/{orderId}/status")
    public Map<String, Object> updateSellerOrderStatus(
            @AuthenticationPrincipal(expression = "member.id") Long sellerMemberId,
            @PathVariable Long orderId,
            @Valid @RequestBody SellerOrderSingleStatusUpdateRequest request
    ) {
        if (sellerMemberId == null) {
            throw new AccessDeniedException("로그인 정보가 없습니다.");
        }

        sellerOrderService.updateSellerOrderStatus(sellerMemberId, orderId, request.getStatus());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "주문상태가 변경되었습니다.");
        return result;
    }
}