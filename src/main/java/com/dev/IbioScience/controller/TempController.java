package com.dev.IbioScience.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.dev.IbioScience.model.product.enums.CouponPolicy;
import com.dev.IbioScience.model.product.enums.DiscountTarget;
import com.dev.IbioScience.model.product.enums.PromotionTerm;
import com.dev.IbioScience.model.product.enums.PromotionType;


@Controller
public class TempController {

	@GetMapping("/categoryManager")
	public String categoryManager() {
		
		return "administration/category/categoryManager";
	}
	
	@GetMapping("/displayManager")
	public String displayManager() {
		
		return "administration/product/displayManager";
	}
	
	@GetMapping("/productManager")
	public String productManager() {
		
		return "administration/product/productManager";
	}
	
	@GetMapping("/productDiscountManager")
    public String productDiscountManager(Model model) {
        model.addAttribute("discountTypes", PromotionType.values());
        model.addAttribute("discountTerms", PromotionTerm.values());
        model.addAttribute("discountTargets", DiscountTarget.values());
        model.addAttribute("couponPolicies", CouponPolicy.values());
        return "administration/product/productDiscountManager";
    }
}
