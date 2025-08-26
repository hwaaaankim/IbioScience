package com.dev.IbioScience.controller.front;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CommonFrontController {

	@GetMapping("/signUp")
	public String signUp() {
		
		return "front/common/signUp";
	}

	@GetMapping("/signIn")
	public String signIn() {
		
		return "front/common/signIn";
	}
	
}
