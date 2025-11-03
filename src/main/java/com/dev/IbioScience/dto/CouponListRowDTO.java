package com.dev.IbioScience.dto;

import java.time.LocalDate;

import com.dev.IbioScience.enums.product.CouponPolicy;
import com.dev.IbioScience.enums.product.CouponStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 쿠폰 리스트 한 행용 DTO (프로모션 개수 포함) */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponListRowDTO {
    private Long id;
    private String couponName;
    private LocalDate startDate;
    private LocalDate endDate;
    private CouponPolicy couponPolicy;
    private CouponStatus status;
    private Long promotionCount; // 0 또는 1 (현재 구조상 1:1 이므로)
}