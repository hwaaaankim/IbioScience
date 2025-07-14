package com.dev.IbioScience.model.product;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.dev.IbioScience.model.product.enums.CouponPolicy;
import com.dev.IbioScience.model.product.enums.CouponStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "tb_coupon")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String couponCode; // 고유 쿠폰코드

    @Column(nullable = false)
    private String couponName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal minPurchaseAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal couponAmount;
    
    @Column(nullable = false)
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    private CouponPolicy couponPolicy;
    
    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", unique = true) // UNIQUE!
    private Promotion promotion;

}