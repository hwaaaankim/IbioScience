package com.dev.IbioScience.controller.admin.product;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dev.IbioScience.dto.CouponListRowDTO;
import com.dev.IbioScience.dto.CouponRegisterRequestDTO;
import com.dev.IbioScience.dto.CouponUpdateRequestDTO;
import com.dev.IbioScience.enums.product.CouponPolicy;
import com.dev.IbioScience.enums.product.CouponStatus;
import com.dev.IbioScience.model.product.Coupon;
import com.dev.IbioScience.service.product.CouponService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
public class CouponManagerController {

	private final CouponService couponService;

	@PreAuthorize("@adminMenuFacade.canCreateByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).PROD_COUPON_MANAGER)")
	@PostMapping("/couponRegister")
	public String registerCoupon(@ModelAttribute CouponRegisterRequestDTO dto, RedirectAttributes redirectAttributes) {
		try {
			Coupon coupon = couponService.registerCoupon(dto);
			redirectAttributes.addFlashAttribute("success", "쿠폰이 정상적으로 등록되었습니다. ID: " + coupon.getId());
			return "redirect:/couponManager";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/couponManager";
		}
	}

	@PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).PROD_COUPON_MANAGER)")
	@GetMapping("/couponManager")
	public String couponManager(@RequestParam(required = false) String name,
			@RequestParam(required = false) CouponPolicy policy, @RequestParam(required = false) CouponStatus status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			@RequestParam(required = false, defaultValue = "0") Integer page, Model model) {
		Page<CouponListRowDTO> result = couponService.getCouponPage(name, policy, status, startDate, endDate, page);

		model.addAttribute("page", result);
		model.addAttribute("name", name);
		model.addAttribute("policy", policy);
		model.addAttribute("status", status);
		model.addAttribute("startDate", startDate);
		model.addAttribute("endDate", endDate);

		model.addAttribute("policyValues", CouponPolicy.values());
		model.addAttribute("statusValues", CouponStatus.values());

		return "administration/product/coupon/couponManager";
	}

	@PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).PROD_COUPON_MANAGER)")
	@GetMapping("/couponDetail/{id}")
	public String detail(@PathVariable Long id, Model model) {
		Coupon coupon = couponService.getDetail(id);
		model.addAttribute("coupon", coupon);
		model.addAttribute("policyValues", CouponPolicy.values());
		model.addAttribute("statusValues", CouponStatus.values());
		return "administration/product/coupon/couponDetail";
	}

	@PreAuthorize("@adminMenuFacade.canUpdateByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).PROD_COUPON_MANAGER)")
	@PostMapping("/couponUpdate")
	public String update(@ModelAttribute CouponUpdateRequestDTO dto, RedirectAttributes ra) {
		couponService.update(dto);
		ra.addFlashAttribute("success", "쿠폰이 수정되었습니다.");
		return "redirect:/couponDetail/" + dto.getId();
	}

	@PreAuthorize("@adminMenuFacade.canViewByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).PROD_COUPON_MANAGER)")
	@GetMapping("/couponInsertForm")
	public String couponInsertForm() {
	    return "administration/product/coupon/couponInsertForm";
	}
}