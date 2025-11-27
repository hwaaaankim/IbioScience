package com.dev.IbioScience.dto.page.productList;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductOptionDto {
    private Long id;
    private String name;        // 옵션명 예: 50ml
    private String value;       // 내부값
    private BigDecimal extraPrice; // 추가금액
    private String sign;        // "PLUS" / "MINUS"
    private Integer sortOrder;
}