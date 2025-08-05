package com.dev.IbioScience.controller.api.product;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.PromotionRegisterRequest;
import com.dev.IbioScience.dto.PromotionSearchDTO;
import com.dev.IbioScience.model.product.Promotion;
import com.dev.IbioScience.model.product.enums.PromotionType;
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
    
    @GetMapping("/api/promotion/search")
    public ResponseEntity<List<PromotionSearchDTO>> searchPromotion(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) PromotionType type,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) Boolean active
    ) {
        // 동적 검색 조건으로 서비스에서 처리
        List<Promotion> list = productPromotionService.searchPromotions(name, type, startDate, endDate, active);
        List<PromotionSearchDTO> result = list.stream().map(PromotionSearchDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}