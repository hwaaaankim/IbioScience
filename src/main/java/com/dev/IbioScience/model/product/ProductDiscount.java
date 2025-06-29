package com.dev.IbioScience.model.product;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.dev.IbioScience.model.product.status.CouponPolicy;
import com.dev.IbioScience.model.product.status.DiscountTarget;
import com.dev.IbioScience.model.product.status.DiscountTerm;
import com.dev.IbioScience.model.product.status.DiscountType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

//할인/증정 정책
@Data
@Entity
@Table(name = "tb_product_discount")
public class ProductDiscount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 활성화여부
    private Boolean active;

    // 타입(DISCOUNT, GIFT)
    @Enumerated(EnumType.STRING)
    private DiscountType type;

    // 기간(한정/상시)
    @Enumerated(EnumType.STRING)
    private DiscountTerm term;

    // 이름(설명)
    private String name;

    // 조건사용여부
    private Boolean conditionEnabled;

    // 시작일
    private LocalDate startDate;

    // 종료일
    private LocalDate endDate;

    // 대상(전체/일반/딜러)
    @Enumerated(EnumType.STRING)
    private DiscountTarget target;

    // 할인율(%)
    private BigDecimal discountPercent;

    // 실제 저장되는 전체 경로 (서버 내부용)
    @Column(length = 512)
    private String iconPath;

    // 프론트 접근용 URL (ex: /upload/...)
    @Column(length = 512)
    private String iconUrl;

    // 쿠폰정책
    @Enumerated(EnumType.STRING)
    private CouponPolicy couponPolicy;
}
