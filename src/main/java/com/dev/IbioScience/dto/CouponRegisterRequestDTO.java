package com.dev.IbioScience.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

// 관리자 쿠폰 등록 DTO
@Data
public class CouponRegisterRequestDTO {
    private String couponName;
    private BigDecimal minPurchaseAmount;
    private BigDecimal couponAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private String couponPolicy;
}