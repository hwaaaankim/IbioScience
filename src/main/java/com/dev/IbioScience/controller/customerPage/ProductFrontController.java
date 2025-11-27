package com.dev.IbioScience.controller.customerPage;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.dev.IbioScience.dto.page.productList.ProductListItemDto;
import com.dev.IbioScience.enums.page.list.ProductSortOption;
import com.dev.IbioScience.service.page.list.FrontProductListService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ProductFrontController {

	private final FrontProductListService productListService;
	private final ObjectMapper objectMapper;
	
	@GetMapping("/productList")
    public String productList(
            @RequestParam(required = false) Long largeId,
            @RequestParam(required = false) Long mediumId,
            @RequestParam(required = false) Long smallId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "CREATED_AT_DESC") ProductSortOption sort,
            Model model
    ) throws JsonProcessingException {

        // keyword 공백 처리
        if (keyword != null) {
            keyword = keyword.trim();
            if (keyword.isEmpty()) {
                keyword = null;
            }
        }

        // 실제 조회
        Page<ProductListItemDto> productPage = productListService.searchProducts(
                largeId,
                mediumId,
                smallId,
                brandId,
                keyword,
                sort,
                page,
                size
        );

        // 뷰로 전달
        model.addAttribute("productPage", productPage);

        // 현재 필터/정렬 상태
        model.addAttribute("largeId", largeId);
        model.addAttribute("mediumId", mediumId);
        model.addAttribute("smallId", smallId);
        model.addAttribute("brandId", brandId);
        model.addAttribute("keyword", keyword);

        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);

     // ✅ JS에서 쓸 수 있도록, content 부분만 JSON 문자열로 변환
        String productListJson = objectMapper.writeValueAsString(productPage.getContent());
        model.addAttribute("productListJson", productListJson);
        
        // 페이지네이션용
        model.addAttribute("pageable", productPage.getPageable());

        // TODO: 필요 시 대분류/브랜드 목록도 추가
        // model.addAttribute("largeCategories", categoryService.getLargeCategories());
        // model.addAttribute("brands", brandService.getAllBrands());

        return "front/product/productList";
    }

    @GetMapping({"/productDetail", "/productDetail/{id}"})
    public String productDetail(
            @PathVariable(required = false) Long id,
            Model model
    ) {

        // id 가 있는 경우 → 상세 조회용으로 model에 추가
        if (id != null) {
            model.addAttribute("productId", id);
            // TODO: 여기서 서비스 호출해서 상세 데이터 조회 가능
            // Product product = productService.getDetail(id);
            // model.addAttribute("product", product);
        }

        // id 가 없어도 그냥 상세 페이지 템플릿으로 이동 가능
        return "front/product/productDetail";
    }
	
	@GetMapping("/dealerProductList")
	public String dealerProductList() {
		
		return "front/product/dealerProductList";
	}
	
}
