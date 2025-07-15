package com.dev.IbioScience.controller.api.product;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.PromotionRegisterRequest;
import com.dev.IbioScience.service.product.ProductPromotionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/promotion")
public class PromotionAPIController {

    private final ProductPromotionService productPromotionService;

    @Value("${spring.upload.path}")
    private String uploadRootPath;

    @PostMapping
    public ResponseEntity<?> registerPromotion(@ModelAttribute PromotionRegisterRequest req) {
        productPromotionService.savePromotion(req);
        return ResponseEntity.ok().build();
    }
}