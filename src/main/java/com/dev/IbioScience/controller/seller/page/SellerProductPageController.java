package com.dev.IbioScience.controller.seller.page;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/seller/page")
public class SellerProductPageController {

    @GetMapping({"", "/", "/main"})
    public String sellerMain() {
        return "redirect:/seller/page/product/insert";
    }

    @GetMapping("/product/insert")
    public String sellerProductInsertForm() {
        return "administration/seller/product/productInsertForm";
    }

    @GetMapping("/product/{dealerProductId}")
    public String sellerProductDetail(@PathVariable Long dealerProductId, Model model) {
        model.addAttribute("dealerProductId", dealerProductId);
        return "administration/seller/product/productDetail";
    }

    @GetMapping("/product/list")
    public String sellerProductList() {
        return "administration/seller/product/productManager";
    }
}