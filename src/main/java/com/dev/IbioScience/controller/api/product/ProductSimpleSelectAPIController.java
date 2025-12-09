package com.dev.IbioScience.controller.api.product;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.front.productList.ProductSearchCondition;
import com.dev.IbioScience.dto.product.select.CategoryDto;
import com.dev.IbioScience.dto.product.select.ProductDetailDto;
import com.dev.IbioScience.dto.product.select.ProductListItemDto;
import com.dev.IbioScience.dto.product.select.ProductReviewDto;
import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.SaleStatus;
import com.dev.IbioScience.service.product.ProductSelectService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/productSelect")
@RequiredArgsConstructor
public class ProductSimpleSelectAPIController {

    private final ProductSelectService productSelectService;

    /** 제품 리스트 조회 */
    @GetMapping("/products")
    public Page<ProductListItemDto> getProducts(
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) DisplayStatus displayStatus,
            @RequestParam(required = false) SaleStatus saleStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) String validOn, // yyyy-MM-dd
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Sort s = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, s);

        ProductSearchCondition cond = new ProductSearchCondition();
        cond.setBrandId(brandId);
        cond.setDisplayStatus(displayStatus);
        cond.setSaleStatus(saleStatus);
        cond.setKeyword(keyword);
        cond.setMinPrice(minPrice);
        cond.setMaxPrice(maxPrice);
        if (validOn != null && !validOn.isBlank()) cond.setValidOn(LocalDate.parse(validOn));

        return productSelectService.searchProducts(cond, pageable);
    }

    /** 제품 상세 조회 */
    @GetMapping("/products/{id}")
    public ProductDetailDto getProductDetail(@PathVariable Long id) {
        return productSelectService.getProductDetail(id);
    }

    /** 특정 제품의 리뷰 목록(페이지) */
    @GetMapping("/products/{id}/reviews")
    public Page<ProductReviewDto> getProductReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Sort s = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, s);
        return productSelectService.getProductReviews(id, pageable);
    }

    /** 일반 카테고리 */
    @GetMapping("/categories/large")
    public List<CategoryDto.Large> listLargeCategories() {
        return productSelectService.listLargeCategories();
    }

    @GetMapping("/categories/large/{largeId}/mediums")
    public List<CategoryDto.Medium> listMediumsByLarge(@PathVariable Long largeId) {
        return productSelectService.listMediumsByLarge(largeId);
    }

    @GetMapping("/categories/medium/{mediumId}/smalls")
    public List<CategoryDto.Small> listSmallsByMedium(@PathVariable Long mediumId) {
        return productSelectService.listSmallsByMedium(mediumId);
    }

    /** 내부 카테고리 */
    @GetMapping("/categories/internal/large")
    public List<CategoryDto.InternalLarge> listInternalLarge() {
        return productSelectService.listInternalLarge();
    }

    @GetMapping("/categories/internal/large/{largeId}/mediums")
    public List<CategoryDto.InternalMedium> listInternalMediums(@PathVariable Long largeId) {
        return productSelectService.listInternalMediumByLarge(largeId);
    }

    @GetMapping("/categories/internal/medium/{mediumId}/smalls")
    public List<CategoryDto.InternalSmall> listInternalSmalls(@PathVariable Long mediumId) {
        return productSelectService.listInternalSmallByMedium(mediumId);
    }

    private Sort parseSort(String sort) {
        // 예: createdAt,desc | salePrice,asc
        if (sort == null || sort.isBlank()) return Sort.by(Sort.Direction.DESC, "createdAt");
        String[] parts = sort.split(",");
        if (parts.length == 2) {
            return Sort.by(Sort.Direction.fromString(parts[1]), parts[0]);
        }
        return Sort.by(Sort.Direction.DESC, parts[0]);
    }
}