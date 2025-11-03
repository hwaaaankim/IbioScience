package com.dev.IbioScience.model.product.review;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.dev.IbioScience.model.product.Product;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
@Table(
    name = "tb_product_review",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_review_product_member", columnNames = {"product_id", "member_id"})
    },
    indexes = {
        @Index(name = "idx_review_product", columnList = "product_id"),
        @Index(name = "idx_review_member", columnList = "member_id"),
        @Index(name = "idx_review_created", columnList = "created_at")
    }
)
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어떤 제품의 리뷰인지 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** 작성자 ID (Member 엔티티 직접 참조는 패키지/구조 확인 후 연결 권장) */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 작성자 표시명(옵션) – 익명/닉네임 노출용 */
    @Column(name = "member_display_name", length = 100)
    private String memberDisplayName;

    /** 별점(1~5) */
    @Column(nullable = false)
    private Integer rating;

    /** 내용(텍스트/HTML 허용 여부는 정책에 따라필터링) */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 작성일/수정일 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 이미지 다중 첨부 */
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductReviewImage> images = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}