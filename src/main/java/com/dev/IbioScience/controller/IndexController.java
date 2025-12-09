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
        // 데이터가 없을 수 있으므로 항상 빈 리스트 안전
        List<ProductCardDTO> topViewed   = productIndexService.topViewed(10);
        List<ProductCardDTO> topSales    = productIndexService.topSales(10);
        List<ProductCardDTO> promoOldest = productIndexService.promotionOldest(10);

        model.addAttribute("idxTopViewed", topViewed);
        model.addAttribute("idxTopSales", topSales);
        model.addAttribute("idxPromoOldest", promoOldest);

        return "front/index";
    }
}
