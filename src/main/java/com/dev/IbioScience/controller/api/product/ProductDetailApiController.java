package com.dev.IbioScience.controller.api.product;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto;
import com.dev.IbioScience.exception.ProductNotDisplayableException;
import com.dev.IbioScience.exception.ProductNotFoundException;
import com.dev.IbioScience.service.product.front.ProductDetailService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductDetailApiController {

    private final ProductDetailService productDetailService;

    @GetMapping("/detail/{id}")
    public ResponseEntity<ProductDetailResponseDto> getDetail(@PathVariable Long id) {
        ProductDetailResponseDto dto = productDetailService.getProductDetail(id);
        return ResponseEntity.ok(dto);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<String> handleNotFound(ProductNotFoundException e) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(ProductNotDisplayableException.class)
    public ResponseEntity<String> handleNotDisplayable(ProductNotDisplayableException e) {
        // 403 또는 404 중 선택 가능. 여기서는 403 사용 예시
        return ResponseEntity.status(403).body("진열하지 않는 상품입니다.");
    }
}