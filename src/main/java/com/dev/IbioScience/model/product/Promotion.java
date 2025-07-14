package com.dev.IbioScience.model.product;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.dev.IbioScience.model.product.enums.PromotionTarget;
import com.dev.IbioScience.model.product.enums.PromotionTerm;
import com.dev.IbioScience.model.product.enums.PromotionType;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

//할인/증정 정책
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

    // 쿠폰발행 프로모션의 경우
    @OneToOne(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Coupon coupon;
}
