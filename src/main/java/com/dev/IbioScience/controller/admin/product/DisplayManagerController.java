package com.dev.IbioScience.controller.admin.product;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class DisplayManagerController {

	@GetMapping("/displayManager")
	public String displayManager() {
		
		return "administration/product/displayManager";
	}

}
