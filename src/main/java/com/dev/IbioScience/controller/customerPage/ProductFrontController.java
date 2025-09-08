package com.dev.IbioScience.controller.customerPage;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProductFrontController {

	@GetMapping("/productList")
	public String productList() {
		
		return "front/product/productList";
	}

	@GetMapping("/productDetail")
	public String productDetail() {
		
		return "front/product/productDetail";
	}
	
	@GetMapping("/dealerProductList")
	public String dealerProductList() {
		
		return "front/product/dealerProductList";
	}
	
}
