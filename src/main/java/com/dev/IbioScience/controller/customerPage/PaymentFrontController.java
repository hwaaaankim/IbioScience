package com.dev.IbioScience.controller.customerPage;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customer")
public class PaymentFrontController {


	@GetMapping("/receipt")
	public String receipt() {

		return "front/payment/receipt";
	}

	@GetMapping("/cart/{id}")
	public String cart(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {

		Long loginMemberId = resolveLoginMemberId();

		// 로그인 안 된 상태면 접근 불가 (헤더에서 sec:authorize로 막더라도, URL 직접 접근 방지)
		if (loginMemberId == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
			return "redirect:/";
		}

		// 본인 장바구니만 허용
		if (id == null || !id.equals(loginMemberId)) {
			redirectAttributes.addFlashAttribute("errorMessage", "잘못된 접근입니다.");
			return "redirect:/";
		}

		// 뷰에서 JS 주입용(= window.__loginMemberId)
		model.addAttribute("cartMemberId", loginMemberId);

		return "front/payment/cart";
	}

	/**
	 * principal에서 member.id 를 최대한 안전하게 꺼내기위한 메서드
	 */
	private Long resolveLoginMemberId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated())
			return null;

		Object principal = auth.getPrincipal();
		if (principal == null)
			return null;

		try {
			BeanWrapper bw = new BeanWrapperImpl(principal);

			// 1) principal.member.id
			if (bw.isReadableProperty("member.id")) {
				Object v = bw.getPropertyValue("member.id");
				return toLong(v);
			}

			// 2) principal.id (혹시 이런 구조일 경우 대비)
			if (bw.isReadableProperty("id")) {
				Object v = bw.getPropertyValue("id");
				return toLong(v);
			}

		} catch (Exception ignored) {
		}

		return null;
	}

	private Long toLong(Object v) {
		if (v == null)
			return null;
		if (v instanceof Long)
			return (Long) v;
		if (v instanceof Integer)
			return ((Integer) v).longValue();
		if (v instanceof Number)
			return ((Number) v).longValue();
		try {
			return Long.parseLong(String.valueOf(v));
		} catch (Exception e) {
			return null;
		}
	}

}
