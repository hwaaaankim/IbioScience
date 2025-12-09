package com.dev.IbioScience.model.product.relation;

import java.time.LocalDateTime;

import com.dev.IbioScience.model.product.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
@Table(
        name = "tb_purchased_product",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_purchased_product_member_product",
                        columnNames = {"member_id", "product_id"}
                )
        },
        indexes = {
                @Index(name = "idx_purchased_member", columnList = "member_id"),
                @Index(name = "idx_purchased_product", columnList = "product_id"),
                @Index(name = "idx_purchased_at", columnList = "purchased_at")
        }
)
public class PurchasedProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 구매한 회원 ID */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 어떤 상품을 샀는지 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** 구매 일시 (마지막 구매 기준 등으로 활용 가능) */
    @Column(name = "purchased_at", nullable = false)
    private LocalDateTime purchasedAt;

    @PrePersist
    protected void onCreate() {
        if (purchasedAt == null) {
            purchasedAt = LocalDateTime.now();
        }
    }
}