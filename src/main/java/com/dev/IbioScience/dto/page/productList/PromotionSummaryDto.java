package com.dev.IbioScience.dto.page.productList;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PromotionSummaryDto {
    private boolean hasPromotion;                 // 어떤 프로모션이라도 있으면 true
    private boolean hasNormalPromotion;           // 일반회원 대상 할인 존재 여부
    private BigDecimal normalDiscountPercent;     // 일반회원 대상 최대 할인율(%)
    private Integer discountedPriceForNormal;     // 일반회원 기준 최종가(정수, 원 단위)

    private boolean hasCouponPromotion;           // 쿠폰 발행형 프로모션 여부
    private boolean hasGiftPromotion;             // 증정형 프로모션 여부
    private boolean hasOnePlusOnePromotion;       // 1+1 프로모션 여부

    private java.util.List<String> couponNames;   // 노출용 쿠폰 이름 목록
}