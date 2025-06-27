package com.dev.IbioScience.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.dev.IbioScience.model.product.status.CouponPolicy;
import com.dev.IbioScience.model.product.status.DiscountTarget;
import com.dev.IbioScience.model.product.status.DiscountTerm;
import com.dev.IbioScience.model.product.status.DiscountType;


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
        model.addAttribute("discountTypes", DiscountType.values());
        model.addAttribute("discountTerms", DiscountTerm.values());
        model.addAttribute("discountTargets", DiscountTarget.values());
        model.addAttribute("couponPolicies", CouponPolicy.values());
        return "administration/product/productDiscountManager";
    }
}
