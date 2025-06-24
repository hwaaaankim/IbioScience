package com.dev.IbioScience.controller.api.product;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.service.category.ProductService;
import com.dev.IbioScience.service.category.ProductService.ProductSimpleDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductAPIController {
    private final ProductService productService;

    @GetMapping("/list-simple")
    public List<ProductSimpleDto> listSimple(@RequestParam Long smallId) {
        return productService.getSimpleProductListBySmallId(smallId);
    }
}
