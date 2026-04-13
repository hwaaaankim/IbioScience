package com.dev.IbioScience.controller.admin.common;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/root")
public class AdminSettlementViewController {

	@PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).SHOP_SETTLEMENT_EXECUTE)")
	@GetMapping("/settlement")
	public String settlementExecutePage() {
	    return "administration/shopManager/settlement/settlement";
	}

	@PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).SHOP_SETTLEMENT_MANAGER)")
	@GetMapping("/settlementManager")
	public String settlementManagerPage() {
	    return "administration/shopManager/settlement/settlementManager";
	}
}