package com.dev.IbioScience.dto.productList;

import java.time.LocalDate;

import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.SaleStatus;

import lombok.Data;

@Data
public class ProductSearchCondition {
    private Long brandId;
    private DisplayStatus displayStatus;
    private SaleStatus saleStatus;
    private String keyword;
    private Integer minPrice;
    private Integer maxPrice;
    private LocalDate validOn;

    private Long largeId;   // (참고) 현재 서비스에서 별도 집합 처리 예정
    private Long mediumId;  // N:N 구조
    private Long smallId;   // N:N 구조
}