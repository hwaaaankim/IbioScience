package com.dev.IbioScience.controller.customerPage;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto;
import com.dev.IbioScience.dto.page.productList.ProductListItemDto;
import com.dev.IbioScience.enums.page.list.ProductSortOption;
import com.dev.IbioScience.exception.ProductNotDisplayableException;
import com.dev.IbioScience.exception.ProductNotFoundException;
import com.dev.IbioScience.service.page.list.FrontProductListService;
import com.dev.IbioScience.service.product.front.ProductDetailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ProductFrontController {

	private final FrontProductListService productListService;
	private final ObjectMapper objectMapper;
	private final ProductDetailService productDetailService;

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

	 /**
     * 제품 상세 페이지
     *
     * - /productDetail/{id} 필수 사용 권장
     * - 잘못된 접근( id 없음 / 존재하지 않는 상품 / 진열 불가 상품 )은
     *   메시지 출력 후 메인으로 redirect
     */
    @GetMapping({"/productDetail", "/productDetail/{id}"})
    public String productDetail(@PathVariable(required = false) Long id,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        // 0) id 없이 직접 /productDetail 접근한 경우
        if (id == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "상품 정보가 존재하지 않습니다.");
            return "redirect:/";
        }

        try {
            // 1) 서비스에서 상세 DTO 조회 + 진열/상태 검사
            ProductDetailResponseDto detail = productDetailService.getProductDetail(id);

            // 2) 뷰에서 사용할 데이터 model 에 세팅
            model.addAttribute("productId", id);        // 필요시 JS 등에서 사용
            model.addAttribute("productDetail", detail); // 타임리프에서 모든 상세 필드 직접 사용

            // 3) 상세 템플릿으로 이동
            return "front/product/productDetail";

        } catch (ProductNotDisplayableException e) {
            // 진열하지 않는 상품 / 판매중지 / 삭제대기/삭제 등
            log.warn("상품 상세 진입 차단 - 진열 불가 상품. id={}, message={}", id, e.getMessage());
            // 예외에 메시지를 넣어두었으니 그대로 노출하거나, 고정 문구로 노출 가능
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage() != null ? e.getMessage() : "진열하지 않는 상품입니다."
            );
            return "redirect:/";

        } catch (ProductNotFoundException e) {
            // 존재하지 않는 상품
            log.warn("상품 상세 진입 실패 - 상품 미존재. id={}", id);
            redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않는 상품입니다.");
            return "redirect:/";

        } catch (Exception e) {
            // 그 외 예기치 못한 오류 (템플릿/서비스/DB 등)
            log.error("상품 상세 진입 중 알 수 없는 오류 발생. id={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "상품 상세 정보를 불러오는 중 오류가 발생했습니다.");
            return "redirect:/";
        }
    }

	@GetMapping("/dealerProductList")
	public String dealerProductList() {

		return "front/product/dealerProductList";
	}
}
