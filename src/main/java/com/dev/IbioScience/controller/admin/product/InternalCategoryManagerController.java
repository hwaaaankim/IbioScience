package com.dev.IbioScience.controller.admin.product;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class InternalCategoryManagerController {

    @PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).PROD_INTERNAL_CATEGORY_MANAGER)")
    @GetMapping("/internalCategoryManager")
    public String internalCategoryManager() {
        return "administration/product/internalCategoryManager";
    }
}