package com.dev.IbioScience.controller.temp;

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

	@GetMapping("/brandManager")
	public String brandManager() {
		
		return "administration/product/brandManager";
	}
	
	@GetMapping("/internalCategoryManager")
	public String internalCategoryManager() {
		
		return "administration/product/internalCategoryManager";
	}
	
}















