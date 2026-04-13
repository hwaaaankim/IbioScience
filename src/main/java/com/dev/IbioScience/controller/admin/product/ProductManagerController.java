package com.dev.IbioScience.controller.admin.product;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class ProductManagerController {

    @PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).PROD_PRODUCT_INSERT)")
    @GetMapping("/productInsertForm")
    public String productInsertForm() {
        return "administration/product/product/productInsertForm";
    }

    @PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).PROD_PRODUCT_LIST)")
    @GetMapping("/productManager")
    public String productManager() {
        return "administration/product/product/productManager";
    }

    @PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).PROD_PRODUCT_LIST)")
    @GetMapping("/productDetail/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        return "administration/product/product/productDetail";
    }

    @PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).PROD_PRODUCT_LIST)")
    @GetMapping("/productUpdate")
    public String productUpdate() {
        return "redirect:/productManager";
    }
}