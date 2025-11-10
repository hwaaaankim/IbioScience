package com.dev.IbioScience.controller.api.menu;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.page.index.BrandSimpleDTO;
import com.dev.IbioScience.dto.page.index.IdNameDTO;
import com.dev.IbioScience.dto.page.index.ProductSimpleDTO;
import com.dev.IbioScience.service.menu.MenuService;

import lombok.RequiredArgsConstructor;

/** /api/menu/** */
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuAPIController {

    private final MenuService menuService;

    @GetMapping("/categories/large")
    public ResponseEntity<List<IdNameDTO>> listLarge() {
        return ResponseEntity.ok(menuService.listLarge());
    }

    @GetMapping("/categories/medium")
    public ResponseEntity<List<IdNameDTO>> listMedium(@RequestParam("largeId") Long largeId) {
        return ResponseEntity.ok(menuService.listMediumByLarge(largeId));
    }

    /** 중:소 (N:N) */
    @GetMapping("/categories/small")
    public ResponseEntity<List<IdNameDTO>> listSmall(@RequestParam("mediumId") Long mediumId) {
        return ResponseEntity.ok(menuService.listSmallByMedium(mediumId));
    }

    @GetMapping("/brands")
    public ResponseEntity<List<BrandSimpleDTO>> listBrands() {
        return ResponseEntity.ok(menuService.listBrands());
    }

    /** 교집합 제품 조회 (largeId / mediumId / smallId / brandId 중 선택) */
    @GetMapping("/products")
    public ResponseEntity<List<ProductSimpleDTO>> listProductsIntersect(
            @RequestParam(value = "largeId", required = false) Long largeId,
            @RequestParam(value = "mediumId", required = false) Long mediumId,
            @RequestParam(value = "smallId", required = false) Long smallId,
            @RequestParam(value = "brandId", required = false) Long brandId
    ) {
        return ResponseEntity.ok(menuService.listProductsIntersect(largeId, mediumId, smallId, brandId));
    }
}