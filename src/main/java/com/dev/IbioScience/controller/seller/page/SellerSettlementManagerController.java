package com.dev.IbioScience.controller.seller.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/seller/settlement")
public class SellerSettlementManagerController {

    @GetMapping("/manager")
    public String settlementManagerPage() {
        return "administration/seller/settlement/settlementManager";
    }
}