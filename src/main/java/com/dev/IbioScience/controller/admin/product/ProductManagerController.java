package com.dev.IbioScience.controller.admin.product;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ProductManagerController {

	@GetMapping("/productInsertForm")
	public String productInsertForm() {
		
		return "administration/product/product/productInsertForm";
	}
	
	@GetMapping("/productManager")
	public String productManager() {
		
		return "administration/product/product/productManager";
	}
	
	@GetMapping("/productDetail/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        return "administration/product/product/productDetail";
    }
	
	@GetMapping("/productUpdate")
	public String productUpdate() {
		
		return "redirect:/productManager";
	}
}
