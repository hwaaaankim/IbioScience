package com.dev.IbioScience.controller.customerPage;

import java.util.ArrayList;
import java.util.List;

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
	public String productList(@RequestParam(required = false) Long largeId,
			@RequestParam(required = false) Long mediumId, @RequestParam(required = false) Long smallId,
			@RequestParam(required = false) Long brandId, @RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "15") int size,
			@RequestParam(defaultValue = "CREATED_AT_DESC") ProductSortOption sort, Model model)
			throws JsonProcessingException {

		// keyword 공백 처리
		if (keyword != null) {
			keyword = keyword.trim();
			if (keyword.isEmpty()) {
				keyword = null;
			}
		}

		// ✅ 실제 조회 (페이지)
		Page<ProductListItemDto> productPage = productListService.searchProducts(largeId, mediumId, smallId, brandId,
				keyword, sort, page, size);

		// ✅ CATEGORY BEST (판매수 상위 10개)
		List<ProductListItemDto> categoryBestList = productListService.findCategoryBestProducts(largeId, mediumId,
				smallId, brandId, keyword);

		// ✅ breadcrumb / 좌측 필터용 텍스트
		String categoryPathText = buildCategoryPathText(largeId, mediumId, smallId);
		String brandLabel = buildBrandLabel(brandId, productPage);

		// 뷰로 전달
		model.addAttribute("productPage", productPage);
		model.addAttribute("categoryBestList", categoryBestList);

		// 현재 필터/정렬 상태
		model.addAttribute("largeId", largeId);
		model.addAttribute("mediumId", mediumId);
		model.addAttribute("smallId", smallId);
		model.addAttribute("brandId", brandId);
		model.addAttribute("keyword", keyword);

		model.addAttribute("page", page);
		model.addAttribute("size", size);
		model.addAttribute("sort", sort);
		model.addAttribute("sortName", sort.name());

		// 텍스트/카운트
		model.addAttribute("categoryPathText", categoryPathText);
		model.addAttribute("brandLabel", brandLabel);
		model.addAttribute("totalCount", productPage.getTotalElements());

		// JS에서 쓸 수 있도록 content 부분만 JSON 문자열로 변환 (필요시 사용)
		String productListJson = objectMapper.writeValueAsString(productPage.getContent());
		model.addAttribute("productListJson", productListJson);

		model.addAttribute("pageable", productPage.getPageable());

		return "front/product/productList";
	}

	/**
	 * breadcrumb / 좌측 카테고리용 간단 텍스트 - 실제 카테고리명 조회 서비스가 있다면 여기에서 ID 대신 이름으로 바꾸시면 됩니다.
	 */
	private String buildCategoryPathText(Long largeId, Long mediumId, Long smallId) {
		List<String> parts = new ArrayList<>();
		if (largeId != null)
			parts.add("대분류:" + largeId);
		if (mediumId != null)
			parts.add("중분류:" + mediumId);
		if (smallId != null)
			parts.add("소분류:" + smallId);

		if (parts.isEmpty()) {
			return "전체 분류";
		}
		return String.join(" > ", parts);
	}

	/**
	 * 브랜드 라벨 - 브랜드 ID만 있고 조회 결과가 없을 수 있으므로, 우선 페이지에서 브랜드명을 읽고, 없으면 "브랜드:ID" 형태로 표시
	 */
	private String buildBrandLabel(Long brandId, Page<ProductListItemDto> productPage) {
		if (brandId == null)
			return null;

		if (productPage != null && !productPage.isEmpty()) {
			ProductListItemDto first = productPage.getContent().get(0);
			if (first.getBrandName() != null) {
				return first.getBrandName();
			}
		}
		return "브랜드:" + brandId;
	}

	@GetMapping({ "/productDetail", "/productDetail/{id}" })
	public String productDetail(@PathVariable(required = false) Long id, Model model) {

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
