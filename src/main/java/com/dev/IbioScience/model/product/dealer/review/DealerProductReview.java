package com.dev.IbioScience.model.product.dealer.review;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.dev.IbioScience.model.product.dealer.DealerProduct;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(
    name = "tb_dealer_product_review",
    indexes = {
        @Index(name = "idx_dealer_review_product", columnList = "dealer_product_id"),
        @Index(name = "idx_dealer_review_member", columnList = "member_id"),
        @Index(name = "idx_dealer_review_created", columnList = "created_at"),
        @Index(name = "idx_dealer_review_product_member", columnList = "dealer_product_id, member_id")
    }
)
public class DealerProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어떤 딜러상품의 리뷰인지 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dealer_product_id", nullable = false)
    private DealerProduct dealerProduct;

    /** 작성자 회원 ID */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 작성자 표시명 */
    @Column(name = "member_display_name", length = 100)
    private String memberDisplayName;

    /** 별점(1~5) */
    @Column(nullable = false)
    private Integer rating;

    /** 리뷰 내용 */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 작성일 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 수정일 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 리뷰 이미지 */
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DealerProductReviewImage> images = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}