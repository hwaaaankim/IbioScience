package com.dev.IbioScience.dto.page.index;

import java.util.List;

import com.dev.IbioScience.enums.product.dealer.ProductSourceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 인덱스(카드)에서 필요한 최소 정보 DTO
 * - 대표이미지 URL
 * - 상품명
 * - 할인 전/후 가격
 * - 판매수/조회수
 * - 평점/리뷰수
 * - 할인률(할인형 프로모션이면)
 * - 비할인 프로모션 라벨(여러 개 가능)
 * - 상품 출처(우리회사/딜러)
 * - 상세 URL
 * - wishlist / recent-view 용 productKey
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCardDTO {

    private Long id;
    private String name;
    private Integer salePrice;
    private Integer consumerPrice;
    private Integer salesCount;
    private Integer viewCount;
    private Double averageRating;
    private Integer reviewCount;
    private Integer discountRate;
    private Integer discountedPrice;
    private String mainImageUrl;
    private List<String> promotionLabels;

    /**
     * COMPANY / DEALER
     */
    private ProductSourceType productSourceType;

    /**
     * "우리회사제품" / "딜러제품"
     */
    private String productSourceLabel;

    /**
     * COMPANY_1 / DEALER_10
     */
    private String productKey;

    /**
     * 프론트 상세 URL
     */
    private String detailUrl;
}