package com.dev.IbioScience.model.product;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.dev.IbioScience.model.product.enums.PromotionTarget;
import com.dev.IbioScience.model.product.enums.PromotionTerm;
import com.dev.IbioScience.model.product.enums.PromotionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "tb_promotion")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String name;

    @Column
    private Boolean conditionEnabled;

    @Column(length = 512)
    private String iconPath;

    @Column(length = 300)
    private String iconUrl;

    @Column
    private Boolean active;

    @Enumerated(EnumType.STRING)
    private PromotionType type;

    @Enumerated(EnumType.STRING)
    private PromotionTerm term;

    @Column
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    @Column(length = 100)
    private String couponName;

    @Column(precision = 6, scale = 2)
    private BigDecimal discountPercent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gift_product_id")
    private Product giftProduct;

    @Enumerated(EnumType.STRING)
    private PromotionTarget target;

    /** 변경 포인트: 여러 프로모션(N) : 하나의 쿠폰(1) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id") // DDL에서 추가한 컬럼
    private Coupon coupon;
}
