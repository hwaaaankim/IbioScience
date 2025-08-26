package com.dev.IbioScience.controller.front;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CommunityFrontController {

	@GetMapping("/eventList")
	public String eventList() {
		
		return "front/community/eventList";
	}

	@GetMapping("/eventDetail")
	public String eventDetail() {
		
		return "front/community/eventDetail";
	}
	
	@GetMapping("/noticeList")
	public String noticeList() {
		
		return "front/community/noticeList";
	}
	
	@GetMapping("/noticeDetail")
	public String noticeDetail() {
		
		return "front/community/noticeDetail";
	}
	
	@GetMapping("/qna")
	public String qna() {
		
		return "front/community/qna";
	}
}
