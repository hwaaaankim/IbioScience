package com.dev.IbioScience.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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
}
