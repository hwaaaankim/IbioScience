package com.dev.IbioScience.controller.customer.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.front.paymentStart.CustomerCouponResponse;
import com.dev.IbioScience.dto.front.paymentStart.CustomerMemberMeResponse;
import com.dev.IbioScience.service.product.front.CustomerPaymentQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/customer/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CustomerPaymentQueryApiController {

	private final CustomerPaymentQueryService customerPaymentQueryService;

	@GetMapping("/member/me")
	public CustomerMemberMeResponse getMyMemberMe() {
		Long memberId = resolveLoginMemberId();
		if (memberId == null) {
			throw new IllegalStateException("로그인이 필요합니다.");
		}
		return customerPaymentQueryService.getMyMemberProfile(memberId);
	}

	/**
	 * ✅ 발급일(issuedAt/createdAt) 범위로 “사용가능(ISSUED)” 쿠폰 조회
	 * - issuedStart, issuedEnd가 없으면 전체 기간
	 * - 없으면 빈 배열 반환 (프론트에서 '없습니다' 표시)
	 */
	@GetMapping("/coupons")
	public List<CustomerCouponResponse> getMyCoupons(
			@RequestParam(value = "issuedStart", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate issuedStart,

			@RequestParam(value = "issuedEnd", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate issuedEnd
	) {
		Long memberId = resolveLoginMemberId();
		if (memberId == null) {
			throw new IllegalStateException("로그인이 필요합니다.");
		}
		return customerPaymentQueryService.getMyUsableCoupons(memberId, issuedStart, issuedEnd);
	}

	// =========================
	// resolveLoginMemberId (환님 제공 코드 그대로 포함)
	// =========================
	private Long resolveLoginMemberId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) return null;

		Object principal = auth.getPrincipal();
		if (principal == null) return null;

		try {
			BeanWrapper bw = new BeanWrapperImpl(principal);

			if (bw.isReadableProperty("member.id")) {
				Object v = bw.getPropertyValue("member.id");
				return toLong(v);
			}

			if (bw.isReadableProperty("id")) {
				Object v = bw.getPropertyValue("id");
				return toLong(v);
			}

		} catch (Exception ignored) {
		}
		return null;
	}

	private Long toLong(Object v) {
		if (v == null) return null;
		if (v instanceof Long) return (Long) v;
		if (v instanceof Integer) return ((Integer) v).longValue();
		if (v instanceof Number) return ((Number) v).longValue();
		try {
			String s = String.valueOf(v).trim();
			if (s.isEmpty()) return null;
			return Long.parseLong(s);
		} catch (Exception e) {
			return null;
		}
	}
}