package com.dev.IbioScience.controller.coupon;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dev.IbioScience.dto.CouponListRowDTO;
import com.dev.IbioScience.dto.CouponUpdateRequestDTO;
import com.dev.IbioScience.model.product.Coupon;
import com.dev.IbioScience.model.product.enums.CouponPolicy;
import com.dev.IbioScience.model.product.enums.CouponStatus;
import com.dev.IbioScience.service.product.CouponService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
public class CouponManagerController {

    private final CouponService couponService;

    /** 리스트 페이지 */
    @GetMapping("/couponManager")
    public String couponManager(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) CouponPolicy policy,
            @RequestParam(required = false) CouponStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            Model model
    ) {
        Page<CouponListRowDTO> result = couponService.getCouponPage(
                name, policy, status, startDate, endDate, page
        );

        model.addAttribute("page", result);
        model.addAttribute("name", name);
        model.addAttribute("policy", policy);
        model.addAttribute("status", status);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        model.addAttribute("policyValues", CouponPolicy.values());
        model.addAttribute("statusValues", CouponStatus.values());

        return "administration/product/coupon/couponManager"; // 템플릿 경로
    }

    /** 상세 페이지 */
    @GetMapping("/couponDetail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Coupon coupon = couponService.getDetail(id);
        model.addAttribute("coupon", coupon);
        model.addAttribute("policyValues", CouponPolicy.values());
        model.addAttribute("statusValues", CouponStatus.values());
        return "administration/product/coupon/couponDetail";
    }
    
    @PostMapping("/couponUpdate")
    public String update(@ModelAttribute CouponUpdateRequestDTO dto,
                         RedirectAttributes ra) {
        couponService.update(dto);
        ra.addFlashAttribute("success", "쿠폰이 수정되었습니다.");
        return "redirect:/couponDetail/" + dto.getId();
    }
    
    /** '쿠폰등록' 버튼용 폼 페이지 라우팅 (사용자 제공 HTML 반환) */
    @GetMapping("/couponInsertForm")
    public String couponInsertForm() {
    	return "administration/product/coupon/couponInsertForm";
    }
}