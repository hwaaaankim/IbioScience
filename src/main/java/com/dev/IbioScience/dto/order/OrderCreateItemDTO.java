package com.dev.IbioScience.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateItemDTO {

    /**
     * COMPANY / DEALER
     */
    private String itemProductType;

    /**
     * 회사상품 ID
     */
    private Long productId;

    /**
     * 딜러상품 ID
     */
    private Long dealerProductId;

    /**
     * 하위 호환용(기존 프론트에서 쓰던 공통 필드)
     * - 회사상품이면 companyOptionGroupId / companyOptionId 로 해석 가능
     * - 딜러상품이면 dealerOptionGroupId / dealerOptionId 로 해석 가능
     */
    private Long optionGroupId;
    private Long optionId;

    /**
     * 회사상품 옵션 참조
     */
    private Long companyOptionGroupId;
    private Long companyOptionId;

    /**
     * 딜러상품 옵션 참조
     */
    private Long dealerOptionGroupId;
    private Long dealerOptionId;

    /**
     * 스냅샷 문자열
     */
    private String productName;
    private String productImageUrl;
    private String optionGroupName;
    private String optionName;
    private String optionCode;
    private String unit;

    /**
     * 금액/수량
     */
    private Long unitPrice;
    private Integer quantity;
    private Long linePrice;
}