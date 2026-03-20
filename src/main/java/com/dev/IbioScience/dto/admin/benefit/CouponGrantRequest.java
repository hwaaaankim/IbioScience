package com.dev.IbioScience.dto.admin.benefit;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.dev.IbioScience.enums.product.CouponPolicy;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CouponGrantRequest {

    @NotBlank
    private String couponCode;

    @NotBlank
    private String couponName;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal minPurchaseAmount;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal couponAmount;

    @NotNull
    private CouponPolicy couponPolicy;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;
}