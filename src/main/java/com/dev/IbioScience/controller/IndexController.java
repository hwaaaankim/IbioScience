package com.dev.IbioScience.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.dev.IbioScience.dto.page.index.ProductCardDTO;
import com.dev.IbioScience.service.product.front.ProductIndexService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class IndexController {

	private final ProductIndexService productIndexService;

	@GetMapping({"/", "", "/index"})
    public String index(Model model) {

        List<ProductCardDTO> topViewed   = productIndexService.topViewed(10);
        List<ProductCardDTO> topSales    = productIndexService.topSales(10);

        // ✅ 이벤트 상품: 없으면 랜덤으로 대체되도록 서비스에서 처리
        List<ProductCardDTO> promoOldest = productIndexService.promotionOldestOrRandom(10);
        System.out.println(promoOldest.size());
        model.addAttribute("idxTopViewed", topViewed);
        model.addAttribute("idxTopSales", topSales);
        model.addAttribute("idxPromoOldest", promoOldest);

        return "front/index";
    }
}
