package com.dev.IbioScience.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
	
	
	

	@GetMapping("/brandManager")
	public String brandManager() {
		
		return "administration/product/brandManager";
	}
	
	@GetMapping("/internalCategoryManager")
	public String internalCategoryManager() {
		
		return "administration/product/internalCategoryManager";
	}
	
}















