package com.dev.IbioScience.model.order;

import java.time.LocalDateTime;

import com.dev.IbioScience.enums.product.dealer.WishListProductType;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.dealer.DealerProduct;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "tb_wishlist_item",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_wishlist_member_company_product",
            columnNames = {"member_id", "product_id"}
        ),
        @UniqueConstraint(
            name = "uk_wishlist_member_dealer_product",
            columnNames = {"member_id", "dealer_product_id"}
        )
    },
    indexes = {
        @Index(name = "ix_wishlist_member", columnList = "member_id"),
        @Index(name = "ix_wishlist_product", columnList = "product_id"),
        @Index(name = "ix_wishlist_dealer_product", columnList = "dealer_product_id"),
        @Index(name = "ix_wishlist_product_type", columnList = "wishlist_product_type"),
        @Index(name = "ix_wishlist_created_at", columnList = "created_at")
    }
)
public class WishListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 찜한 회원 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 찜 상품 타입 (회사상품 / 딜러상품) */
    @Enumerated(EnumType.STRING)
    @Column(name = "wishlist_product_type", nullable = false, length = 20)
    private WishListProductType wishlistProductType;

    /** 우리 회사 상품 (회사상품 찜인 경우만 사용) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    /** 딜러 등록 상품 (딜러상품 찜인 경우만 사용) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_product_id")
    private DealerProduct dealerProduct;

    /** 생성일 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 수정일 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        validateProductReference();

        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        validateProductReference();
        this.updatedAt = LocalDateTime.now();
    }

    private void validateProductReference() {
        if (wishlistProductType == null) {
            throw new IllegalStateException("wishlistProductType 는 필수입니다.");
        }

        if (wishlistProductType == WishListProductType.COMPANY) {
            if (product == null) {
                throw new IllegalStateException("회사상품 찜은 product 가 반드시 있어야 합니다.");
            }
            if (dealerProduct != null) {
                throw new IllegalStateException("회사상품 찜에는 dealerProduct 가 있으면 안 됩니다.");
            }
        }

        if (wishlistProductType == WishListProductType.DEALER) {
            if (dealerProduct == null) {
                throw new IllegalStateException("딜러상품 찜은 dealerProduct 가 반드시 있어야 합니다.");
            }
            if (product != null) {
                throw new IllegalStateException("딜러상품 찜에는 product 가 있으면 안 됩니다.");
            }
        }
    }
}