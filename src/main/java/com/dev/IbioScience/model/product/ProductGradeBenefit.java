package com.dev.IbioScience.model.product;

import java.math.BigDecimal;

import com.dev.IbioScience.model.product.status.DealerGrade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

//등급별 혜택(할인 등)
@Data
@Entity
@Table(name = "tb_product_grade_benefit", 
    uniqueConstraints = @UniqueConstraint(columnNames = { "product_id", "dealer_grade" }))
public class ProductGradeBenefit {

    // 혜택 ID, PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 소속 제품
    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    // 딜러등급 (A/B/C/D 등)
    @Enumerated(EnumType.STRING)
    @Column(name = "dealer_grade")
    private DealerGrade dealerGrade;

    // 할인율
    private BigDecimal discountRate;
}