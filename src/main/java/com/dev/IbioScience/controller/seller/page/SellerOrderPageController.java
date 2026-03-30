package com.dev.IbioScience.controller.seller.page;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/seller")
public class SellerOrderPageController {

    @GetMapping("/orderManager")
    public String orderManager() {
        return "administration/seller/order/orderManager";
    }

    @GetMapping("/orderDetail/{orderId}")
    public String orderDetail(@PathVariable Long orderId, Model model) {
        model.addAttribute("orderId", orderId);
        return "administration/seller/order/orderDetail";
    }
}