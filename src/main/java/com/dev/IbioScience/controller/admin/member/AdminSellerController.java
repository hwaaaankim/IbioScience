package com.dev.IbioScience.controller.admin.member;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.dev.IbioScience.enums.auth.DealerGrade;
import com.dev.IbioScience.enums.auth.MemberStatus;
import com.dev.IbioScience.enums.product.OrganizationCategory;
import com.dev.IbioScience.enums.product.SupplyStructure;
import com.dev.IbioScience.enums.product.SupplyType;
import com.dev.IbioScience.enums.product.TradingStatus;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/root")
@RequiredArgsConstructor
public class AdminSellerController {

	@PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).MEM_SELLER_INSERT_FORM)")
	@GetMapping("/sellerInsertForm")
	public String sellerInsertForm(Model model) {
	    model.addAttribute("memberStatuses", MemberStatus.values());
	    model.addAttribute("organizationCategories", OrganizationCategory.values());
	    model.addAttribute("dealerGrades", DealerGrade.values());
	    model.addAttribute("tradingStatuses", TradingStatus.values());
	    model.addAttribute("supplyTypes", SupplyType.values());
	    model.addAttribute("supplyStructures", SupplyStructure.values());

	    return "administration/clientManager/sellerInsertForm";
	}
}
