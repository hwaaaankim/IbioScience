package com.dev.IbioScience.dto.front.productList;

import java.util.Map;
import java.util.Set;

import com.dev.IbioScience.enums.product.PromotionType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductListRowDTO {
	private Long id;

	// 화면 “자체코드” 컬럼에서 사용
	private String internalProductCode;

	// 품목코드
	private String code;

	// 화면 “제품분류” 컬럼에서 사용 (모드에 따라 INTERNAL/EXTERNAL 요약 문자열)
	private String categorySummary;

	// 대표이미지 URL
	private String imageUrl;

	// 제품명
	private String name;

	// 가격
	private Integer consumerPrice;
	private Integer salePrice;

	// 딜러가(A/B/C/D)
	private Map<String, Integer> dealerPrices;

	// 프로모션 타입들
	private Set<PromotionType> promotionTypes;
}