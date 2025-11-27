package com.dev.IbioScience.dto.page.productList;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class DealerDiscountDto {
    private String dealerGrade;         // "A", "B", ...
    private BigDecimal discountRate;    // 등급별 할인율 (%)
}
