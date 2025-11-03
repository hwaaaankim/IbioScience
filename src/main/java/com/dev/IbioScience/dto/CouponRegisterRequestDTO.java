package com.dev.IbioScience.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.dev.IbioScience.enums.product.CouponPolicy;
import com.dev.IbioScience.enums.product.CouponStatus;

import lombok.Data;

// 관리자 쿠폰 등록 DTO
@Data
public class CouponRegisterRequestDTO {
	private String couponName;                 // 쿠폰명
    private BigDecimal minPurchaseAmount;      // 최소 결제금액
    private BigDecimal couponAmount;           // 할인 금액
    private LocalDate startDate;               // 시작일
    private LocalDate endDate;                 // 종료일
    private CouponPolicy couponPolicy;         // 정책
    private CouponStatus status;               // 상태
}