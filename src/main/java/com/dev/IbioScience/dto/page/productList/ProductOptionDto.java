package com.dev.IbioScience.dto.page.productList;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductOptionDto {
    private Long id;
    private String name;        // 옵션명 예: 50ml
    private String value;       // 내부값 (Cat.No 등으로 사용 가능)
    private BigDecimal extraPrice; // 추가금액
    private String sign;        // "PLUS" / "MINUS"
    private Integer sortOrder;

    // ✅ 옵션별 최종 가격 (기준가 + ± extraPrice)
    private Integer finalPrice;
}