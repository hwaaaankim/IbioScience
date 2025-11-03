package com.dev.IbioScience.dto.productList;

import java.util.Map;
import java.util.Set;

import com.dev.IbioScience.enums.product.PromotionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class ProductListRowDTO {
    private Long id;
    private String internalProductCode;
    private String code;  
    private String categoryPath;               // 내부: "대>중>소"
    private String externalCategorySummary;    // 외부: "대>중>소 외 N개"
    private String imageUrl;         // 대표이미지 URL (없으면 null)
    private String name;
    private Integer consumerPrice;
    private Integer salePrice;

    // 딜러가(등급별) - A/B/C/D 등급별 금액
    private Map<String, Integer> dealerPrices; // ex) {"A":90000,"B":95000,"C":100000,"D":100000}

    // 프로모션 타입 라벨들 (활성 기준)
    private Set<PromotionType> promotionTypes;
}