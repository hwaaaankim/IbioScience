package com.dev.IbioScience.dto.page.productList;

import java.util.List;

import com.dev.IbioScience.enums.product.PriceExposeTarget;
import com.dev.IbioScience.enums.product.ProductNewState;

import lombok.Data;

@Data
public class ProductListItemDto {

    private Long id;

    private String name;                 // 상품명
    private String summaryDescription;   // 요약설명
    private String shortDescription;     // 간략설명
    private String brandName;            // 브랜드명

    private String mainImageUrl;         // 대표 이미지 URL

    private Integer consumerPrice;       // 소비자가
    private Integer salePrice;           // 기본 판매가

    // 가격 노출 정책
    private PriceExposeTarget priceExposeTarget;   // MEMBER / GUEST
    private Boolean usePriceReplacementText;       // 대체문구 사용 여부
    private String priceReplacementText;           // "견적문의" 등

    private ProductNewState newState;    // NEW / STOCK / DISPLAY

    // 평점/리뷰
    private Double averageRating;
    private Long reviewCount;

    // 딜러별 할인 정보
    private List<DealerDiscountDto> dealerDiscounts;

    // 일반회원/프로모션 정보
    private PromotionSummaryDto promotionSummary;

    // 옵션 그룹 + 옵션
    private List<ProductOptionGroupDto> optionGroups;
}