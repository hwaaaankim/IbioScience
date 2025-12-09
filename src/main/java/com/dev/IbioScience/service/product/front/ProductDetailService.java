package com.dev.IbioScience.service.product.front;

import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto;

public interface ProductDetailService {

    /**
     * 상세 페이지에 노출 가능한 상품인지 검사하고,
     * 노출 가능한 경우 전체 상세정보 DTO 반환.
     *
     * - 진열 OFF / 삭제대기 / 삭제 / 판매중지 등은 ProductNotDisplayableException 발생
     * - 상품이 없으면 ProductNotFoundException 발생
     */
    ProductDetailResponseDto getProductDetail(Long productId);
}