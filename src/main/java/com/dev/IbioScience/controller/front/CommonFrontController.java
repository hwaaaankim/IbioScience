package com.dev.IbioScience.controller.front;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CommonFrontController {

	@GetMapping("/personalSignUp")
	public String personalSignUp() {
		
		return "front/common/personalSignUp";
	}
	
	@GetMapping("/companySignUp")
	public String companySignUp() {
		
		return "front/common/companySignUp";
	}

	@GetMapping("/signIn")
	public String signIn() {
		
		return "front/common/signIn";
	}
	
	@GetMapping("/signUpSuccess")
	public String signUpSuccess() {
		
		return "front/common/signUpSuccess";
	}
	
}
