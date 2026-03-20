package com.dev.IbioScience.dto.admin.benefit;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.dev.IbioScience.enums.product.CouponStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CouponRowResponse {

    private Long memberCouponId;
    private String couponName;
    private LocalDate startDate;
    private LocalDate endDate;
    private CouponStatus status;
    private String statusLabel;
    private String sourceText;
    private String usedOrderNo;
    private LocalDateTime issuedAt;
}