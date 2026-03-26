package com.dev.IbioScience.model.product.dealer.review;

import java.time.LocalDateTime;

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
import lombok.Data;

@Data
@Entity
@Table(
    name = "tb_dealer_product_review_image",
    indexes = {
        @Index(name = "idx_dealer_review_img_review", columnList = "review_id"),
        @Index(name = "idx_dealer_review_img_sort", columnList = "sort_order")
    }
)
public class DealerProductReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어떤 리뷰의 이미지인지 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private DealerProductReview review;

    /** 외부 접근 URL */
    @Column(length = 500)
    private String url;

    /** 실제 저장 경로 */
    @Column(length = 500)
    private String path;

    @Column(length = 255)
    private String fileName;

    @Column(length = 255)
    private String originalFilename;

    private Integer size;

    @Column(name = "sort_order")
    private Integer sortOrder;

    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }
}