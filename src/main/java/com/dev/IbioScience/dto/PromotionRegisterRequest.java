package com.dev.IbioScience.dto;

import java.math.BigDecimal;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

// 프로모션 등록용 DTO
@Data
public class PromotionRegisterRequest {
    private String name;
    private String status;     // "ACTIVE" | "INACTIVE"
    private String term;       // "PERIOD" | "ALWAYS"
    private String type;       // "DISCOUNT" | "GIFT" | "ONE_PLUS_ONE" | "COUPON"
    private String startDate;
    private String endDate;

    private BigDecimal discountPercent;   // DISCOUNT 전용
    private Long giftProductId;           // GIFT 전용
    private Long couponId;                // COUPON 전용

    private MultipartFile iconFile;       // 공통
}