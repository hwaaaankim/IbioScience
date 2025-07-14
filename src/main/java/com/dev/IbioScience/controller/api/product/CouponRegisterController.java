package com.dev.IbioScience.controller.api.product;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dev.IbioScience.dto.CouponRegisterRequestDTO;
import com.dev.IbioScience.service.product.CouponService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CouponRegisterController {

	private final CouponService couponService;
	
	@PostMapping("/couponRegister")
    public String couponRegister(
            @ModelAttribute @Valid CouponRegisterRequestDTO dto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "입력값을 확인하세요.");
            return "administration/product/couponManager";
        }
        try {
            couponService.registerCoupon(dto);
            redirectAttributes.addFlashAttribute("success", true);
            return "redirect:/administration/product/couponManager";
        } catch (Exception e) {
            model.addAttribute("error", "쿠폰 등록 중 오류: " + e.getMessage());
            return "administration/product/couponManager";
        }
    }
}
