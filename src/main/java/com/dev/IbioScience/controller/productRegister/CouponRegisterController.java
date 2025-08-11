package com.dev.IbioScience.controller.productRegister;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dev.IbioScience.dto.CouponRegisterRequestDTO;
import com.dev.IbioScience.model.product.Coupon;
import com.dev.IbioScience.service.product.CouponService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CouponRegisterController {

	private final CouponService couponService;
	
	@PostMapping("/couponRegister")
    public String registerCoupon(
    		@ModelAttribute CouponRegisterRequestDTO dto, 
    		RedirectAttributes redirectAttributes) {
        try {
            Coupon coupon = couponService.registerCoupon(dto);
            redirectAttributes.addFlashAttribute("success", "쿠폰이 정상적으로 등록되었습니다. ID: " + coupon.getId());
            return "redirect:/couponManager";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/couponManager";
        }
    }

}
