package com.dev.IbioScience.controller.test;

import org.apache.commons.math3.stat.descriptive.summary.Product;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TESTController {

	@GetMapping("/enumTest")
	public String enumTest(Product p) {
		
		return p.toString();
	}
}
