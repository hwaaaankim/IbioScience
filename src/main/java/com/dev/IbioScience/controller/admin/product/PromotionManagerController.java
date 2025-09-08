package com.dev.IbioScience.controller.admin.product;

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

import com.dev.IbioScience.dto.PromotionRegisterRequest;
import com.dev.IbioScience.model.product.Promotion;
import com.dev.IbioScience.model.product.enums.PromotionTerm;
import com.dev.IbioScience.model.product.enums.PromotionType;
import com.dev.IbioScience.service.product.ProductPromotionService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class PromotionManagerController {

    private final ProductPromotionService productPromotionService;

    /** 기존 등록 폼 */
    @GetMapping("/productPromotionInsertForm")
    public String productDiscountManager(Model model) {
        model.addAttribute("promotionTypes", PromotionType.values());
        model.addAttribute("promotionTerms", PromotionTerm.values());
        return "administration/product/promotion/productPromotionInsertForm";
    }

    /** 1) 매니저 목록 */
    @GetMapping("/productPromotionManager")
    public String promotionManager(@RequestParam(required = false) String name,
                                   @RequestParam(required = false) Boolean active,
                                   @RequestParam(required = false) PromotionType type,
                                   @RequestParam(required = false) PromotionTerm term,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   Model model) {

        Page<Promotion> result = productPromotionService.getPromotionPage(
                name, active, term, startDate, endDate, type, page, size
        );

        model.addAttribute("page", result);
        model.addAttribute("name", name);
        model.addAttribute("active", active);
        model.addAttribute("type", type);
        model.addAttribute("term", term);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        model.addAttribute("promotionTypes", PromotionType.values());
        model.addAttribute("promotionTerms", PromotionTerm.values());
        return "administration/product/promotion/productPromotionManager";
    }

    /** 2) 상세 페이지 */
    @GetMapping("/productPromotionDetail/{id}")
    public String promotionDetail(@PathVariable Long id, Model model) {
        Promotion p = productPromotionService.getOne(id);
        model.addAttribute("promotion", p);
        model.addAttribute("promotionTypes", PromotionType.values());
        model.addAttribute("promotionTerms", PromotionTerm.values());
        return "administration/product/promotion/productPromotionDetail";
    }

    /** 3) 업데이트 */
    @PostMapping("/productPromotionUpdate")
    public String promotionUpdate(@RequestParam Long id,
                                  @ModelAttribute PromotionRegisterRequest req,
                                  RedirectAttributes ra) {
        productPromotionService.updatePromotion(id, req);
        ra.addFlashAttribute("message", "수정되었습니다. (해당 프로모션이 등록된 모든 제품에 적용됩니다)");
        return "redirect:/productPromotionDetail/" + id;
    }

    /** 삭제 */
    @PostMapping("/productPromotionDelete/{id}")
    public String promotionDelete(@PathVariable Long id, RedirectAttributes ra) {
        try {
        	productPromotionService.delete(id);
            ra.addFlashAttribute("message", "삭제되었습니다.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "삭제 중 오류가 발생했습니다.");
        }
        return "redirect:/productPromotionManager";
    }
}