package com.dev.IbioScience.controller.front;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaymentFrontController {

	@GetMapping("/paymentStart")
	public String paymentStart() {
		
		return "front/payment/paymentStart";
	}

	@GetMapping("/paymentEnd")
	public String paymentEnd() {
		
		return "front/payment/paymentEnd";
	}
	
	@GetMapping("/cart")
	public String cart() {
		
		return "front/payment/cart";
	}

	@GetMapping("/receipt")
	public String receipt() {
		
		return "front/payment/receipt";
	}
	
}
