package com.dev.IbioScience.controller.customerPage;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.dev.IbioScience.dto.page.productList.ProductSortOption;

@Controller
public class ProductFrontController {


	 /**
     * 제품 리스트 페이지 진입
     *
     * - largeId / mediumId / smallId / brandId : 선택된 분류/브랜드 (없으면 null)
     * - keyword : 검색 키워드(제품명 등), 없으면 null
     * - page  : 0부터 시작하는 페이지 인덱스, 기본 0
     * - size  : 페이지 당 개수, 기본 15
     * - sort  : 정렬 옵션 (ProductSortOption), 기본 CREATED_AT_DESC (등록일 최신순)
     *
     * 실제 DB 조회는 나중에 구현하고, 지금은 파라미터를 화면으로 넘겨
     * 리스트 페이지에서 현재 조건/정렬/페이지 사이즈/키워드를 사용할 수 있게만 구성.
     */
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
    ) {

        // --- keyword 공백 처리 (공백이면 null로 정리) ---
        if (keyword != null) {
            keyword = keyword.trim();
            if (keyword.isEmpty()) {
                keyword = null;
            }
        }

        // --- 정렬 기준에 맞는 Pageable 미리 준비 (실제 조회 시 그대로 사용 가능) ---
        Sort sortSpec;
        switch (sort) {
            case NAME_ASC:
                sortSpec = Sort.by("name").ascending();
                break;
            case NAME_DESC:
                sortSpec = Sort.by("name").descending();
                break;
            case PRICE_ASC:
                sortSpec = Sort.by("salePrice").ascending(); // 실제 컬럼명에 맞게 수정
                break;
            case PRICE_DESC:
                sortSpec = Sort.by("salePrice").descending(); // 실제 컬럼명에 맞게 수정
                break;
            case RATING_DESC:
                sortSpec = Sort.by("averageRating").descending(); // 실제 컬럼명에 맞게 수정
                break;
            case RATING_ASC:
                sortSpec = Sort.by("averageRating").ascending(); // 실제 컬럼명에 맞게 수정
                break;
            case CREATED_AT_DESC:
            default:
                sortSpec = Sort.by("createdAt").descending(); // 등록일 기준, 최신순
                break;
        }

        Pageable pageable = PageRequest.of(page, size, sortSpec);

        // --- TODO: 실제 조회 로직 (나중에 구현) ---
        //   대/중/소/브랜드 + 키워드를 모두 AND 조건으로 사용한다는 전제
        //
        // Page<Product> productPage = productService.searchProducts(
        //         largeId, mediumId, smallId, brandId, keyword, pageable
        // );
        //
        // model.addAttribute("productPage", productPage);

        // --- 현재 선택/조건 정보 뷰에 전달 (정렬/페이지 사이즈/분류/키워드 상태 표시용) ---
        model.addAttribute("largeId", largeId);
        model.addAttribute("mediumId", mediumId);
        model.addAttribute("smallId", smallId);
        model.addAttribute("brandId", brandId);
        model.addAttribute("keyword", keyword);

        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("pageable", pageable);

        // 향후 분류/브랜드 선택 UI를 위해서 대분류/브랜드 목록도 같이 넘길 수 있음
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
