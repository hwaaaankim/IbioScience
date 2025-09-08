package com.dev.IbioScience.controller.customerPage;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CustomerFrontController {

	@GetMapping("/couponList")
	public String couponList() {
		
		return "front/customer/couponList";
	}
	
	@GetMapping("/estimateList")
	public String estimateList() {
		
		return "front/customer/estimateList";
	}
	
	@GetMapping("/estimate")
	public String estimate() {
		
		return "front/customer/estimate";
	}
	
	@GetMapping("/exchangeReturnList")
	public String exchangeReturnList() {
		
		return "front/customer/exchangeReturnList";
	}
	
	@GetMapping("/exchangeReturn")
	public String exchangeReturn() {
		
		return "front/customer/exchangeReturn";
	}
	
	@GetMapping("/personalInfoUpdate")
	public String personalInfoUpdate() {
		
		return "front/customer/personalInfoUpdate";
	}
	
	@GetMapping("/companyInfoUpdate")
	public String companyInfoUpdate() {
		
		return "front/customer/companyInfoUpdate";
	}
	
	@GetMapping("/inquiry")
	public String inquiry() {
		
		return "front/customer/inquiry";
	}
	
	@GetMapping("/inquiryList")
	public String inquiryList() {
		
		return "front/customer/inquiryList";
	}
	
	@GetMapping("/myPage")
	public String myPage() {
		
		return "front/customer/myPage";
	}
	
	@GetMapping("/orderList")
	public String orderList() {
		
		return "front/customer/orderList";
	}
	
	@GetMapping("/pointList")
	public String pointList() {
		
		return "front/customer/pointList";
	}
	
	@GetMapping("/reviewList")
	public String reviewList() {
		
		return "front/customer/reviewList";
	}
	
	@GetMapping("/wishList")
	public String wishList() {
		
		return "front/customer/wishList";
	}
	
}
