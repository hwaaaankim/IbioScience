package com.dev.IbioScience.controller.admin.member;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.dev.IbioScience.dto.admin.order.AdminOrderSearchRequest;
import com.dev.IbioScience.enums.order.OrderStatus;
import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.enums.order.PaymentMethod;
import com.dev.IbioScience.enums.order.ShippingMethod;
import com.dev.IbioScience.enums.order.ShippingPayType;
import com.dev.IbioScience.model.order.Order;
import com.dev.IbioScience.service.auth.admin.order.AdminOrderManagerService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/root")
@RequiredArgsConstructor
public class AdminOrderManagerController {

    private final AdminOrderManagerService adminOrderManagerService;

    @PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).ORDER_MANAGER)")
    @GetMapping("/orderManager")
    public String orderManager(
            @ModelAttribute("search") AdminOrderSearchRequest search,
            Model model
    ) {
        search.normalize();

        Page<Order> orderPage = adminOrderManagerService.getOrderPage(search);

        int currentPage = orderPage.getNumber() + 1;
        int totalPages = orderPage.getTotalPages();
        int blockStart = totalPages == 0 ? 1 : ((currentPage - 1) / 5) * 5 + 1;
        int blockEnd = totalPages == 0 ? 1 : Math.min(blockStart + 4, totalPages);

        model.addAttribute("orderPage", orderPage);
        model.addAttribute("blockStart", blockStart);
        model.addAttribute("blockEnd", blockEnd);

        model.addAttribute("dealerTypeValues", DealerType.values());
        model.addAttribute("orderStatusValues", OrderStatus.values());
        model.addAttribute("paymentMethodValues", PaymentMethod.values());
        model.addAttribute("shippingMethodValues", ShippingMethod.values());
        model.addAttribute("shippingPayTypeValues", ShippingPayType.values());

        model.addAttribute("dealerTypeLabelMap", adminOrderManagerService.getDealerTypeLabelMap());
        model.addAttribute("orderStatusLabelMap", adminOrderManagerService.getOrderStatusLabelMap());
        model.addAttribute("paymentMethodLabelMap", adminOrderManagerService.getPaymentMethodLabelMap());
        model.addAttribute("shippingMethodLabelMap", adminOrderManagerService.getShippingMethodLabelMap());
        model.addAttribute("shippingPayTypeLabelMap", adminOrderManagerService.getShippingPayTypeLabelMap());

        return "administration/orderManager/orderManager";
    }

    @PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).ORDER_MANAGER)")
    @GetMapping("/orderDetail/{id}")
    public String orderDetail(
            @PathVariable Long id,
            Model model
    ) {
        Order order = adminOrderManagerService.getOrderDetail(id);

        model.addAttribute("order", order);
        model.addAttribute("dealerTypeLabelMap", adminOrderManagerService.getDealerTypeLabelMap());
        model.addAttribute("orderStatusLabelMap", adminOrderManagerService.getOrderStatusLabelMap());
        model.addAttribute("paymentMethodLabelMap", adminOrderManagerService.getPaymentMethodLabelMap());
        model.addAttribute("shippingMethodLabelMap", adminOrderManagerService.getShippingMethodLabelMap());
        model.addAttribute("shippingPayTypeLabelMap", adminOrderManagerService.getShippingPayTypeLabelMap());

        model.addAttribute("orderStatusValues", OrderStatus.values());
        model.addAttribute("paymentMethodValues", PaymentMethod.values());
        model.addAttribute("shippingMethodValues", ShippingMethod.values());
        model.addAttribute("shippingPayTypeValues", ShippingPayType.values());

        return "administration/orderManager/orderDetail";
    }
}