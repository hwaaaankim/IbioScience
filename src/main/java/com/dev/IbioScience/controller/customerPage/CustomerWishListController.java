package com.dev.IbioScience.controller.customerPage;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.dev.IbioScience.dto.order.WishListProductViewDto;
import com.dev.IbioScience.enums.product.SaleStatus;
import com.dev.IbioScience.service.order.WishListService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/customer")
public class CustomerWishListController {

    private final WishListService wishListService;

    @GetMapping(value = {"/wishList/{id}", "/wishList"})
    public String wishList(
            @PathVariable(required = false) Long id,
            @AuthenticationPrincipal(expression = "member.id") Long loginMemberId,

            @RequestParam(name = "sale", required = false, defaultValue = "all") String sale,
            @RequestParam(name = "size", required = false, defaultValue = "10") Integer size,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,

            Model model
    ) {
        if (loginMemberId == null) return "redirect:/";
        if (id != null && !id.equals(loginMemberId)) return "redirect:/";

        int fixedSize = (size == null ? 10 : size);
        if (!(fixedSize == 10 || fixedSize == 30 || fixedSize == 50 || fixedSize == 70 || fixedSize == 100)) {
            fixedSize = 10;
        }

        int fixedPage = (page == null || page < 0) ? 0 : page;

        SaleStatus filter = null;
        if ("on".equalsIgnoreCase(sale)) filter = SaleStatus.ON;
        else if ("off".equalsIgnoreCase(sale)) filter = SaleStatus.OFF;

        Pageable pageable = PageRequest.of(fixedPage, fixedSize);

        Page<WishListProductViewDto> wishPage =
                wishListService.getWishListPage(loginMemberId, filter, pageable);

        model.addAttribute("wishPage", wishPage);
        model.addAttribute("sale", sale);
        model.addAttribute("size", fixedSize);
        model.addAttribute("page", fixedPage);

        return "front/customer/wishList";
    }
}