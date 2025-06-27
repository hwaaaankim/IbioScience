package com.dev.IbioScience.controller.api.product;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.ProductDiscountSaveRequest;
import com.dev.IbioScience.service.product.ProductDiscountService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/product-discount")
public class ProductDiscountAPIController {

	private final ProductDiscountService productDiscountService;

    @PostMapping
    public ResponseEntity<?> saveDiscount(@ModelAttribute ProductDiscountSaveRequest req) {
        try {
            productDiscountService.saveDiscount(req);
            return ResponseEntity.ok().body("success");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
