package com.dev.IbioScience.dto.page.index;

import java.util.List;

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
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCardDTO {
    private Long id;
    private String name;

    private Integer salePrice;       // 판매가(원가가 아님)
    private Integer consumerPrice;   // 소비자가(할인 전 표시용)

    private Integer salesCount;
    private Integer viewCount;

    private Double averageRating;    // 소수점 1자리까지 반환
    private Integer reviewCount;

    private Integer discountRate;    // null 이면 할인 없음
    private Integer discountedPrice; // null 이면 할인 없음

    private String mainImageUrl;     // 대표 이미지 URL (없으면 null)

    private List<String> promotionLabels; // 할인 외 프로모션 이름들
}