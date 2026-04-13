package com.dev.IbioScience.controller.admin.product;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class BrandManagerController {

	@GetMapping("/brandManager")
	@PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).PROD_BRAND_MANAGER)")
	public String brandManager() {
		
		return "administration/product/brandManager";
	}
}
