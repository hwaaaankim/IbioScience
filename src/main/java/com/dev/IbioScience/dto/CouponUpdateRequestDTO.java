package com.dev.IbioScience.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.dev.IbioScience.enums.product.CouponPolicy;
import com.dev.IbioScience.enums.product.CouponStatus;

import lombok.Data;

// 쿠폰 수정용 DTO
@Data
public class CouponUpdateRequestDTO {
    private Long id;
    private String couponName;
    private BigDecimal minPurchaseAmount;
    private BigDecimal couponAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private CouponPolicy couponPolicy;
    private CouponStatus status;
}