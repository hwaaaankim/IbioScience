package com.dev.IbioScience.controller.admin.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/common")
public class AdminCommonController {

	@GetMapping(value = {"/", "", "/main"})
	public String adminMain() {
		
		return "administration/index";
	}
	
}
