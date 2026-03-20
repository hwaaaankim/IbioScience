package com.dev.IbioScience.repository.auth.review;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.model.product.review.ProductReview;

public interface AdminClientReviewRepository extends JpaRepository<ProductReview, Long> {

    @Query(value = """
            SELECT
                r.id AS reviewId,
                r.product_id AS productId,
                r.member_id AS memberId,
                COALESCE(NULLIF(TRIM(r.member_display_name), ''), m.name, CONCAT('회원#', r.member_id)) AS authorName,
                r.rating AS rating,
                r.created_at AS createdAt,
                (
                    SELECT ri.url
                    FROM tb_product_review_image ri
                    WHERE ri.review_id = r.id
                    ORDER BY COALESCE(ri.sort_order, 2147483647) ASC, ri.id ASC
                    LIMIT 1
                ) AS firstImageUrl,
                (
                    SELECT COUNT(1)
                    FROM tb_product_review_image ri2
                    WHERE ri2.review_id = r.id
                ) AS imageCount,
                r.content AS content
            FROM tb_product_review r
            LEFT JOIN member m
                ON m.id = r.member_id
            WHERE r.member_id = :memberId
              AND (:fromDateTime IS NULL OR r.created_at >= :fromDateTime)
              AND (:toDateTime IS NULL OR r.created_at < :toDateTime)
            ORDER BY r.created_at DESC, r.id DESC
            """,
            countQuery = """
            SELECT COUNT(1)
            FROM tb_product_review r
            WHERE r.member_id = :memberId
              AND (:fromDateTime IS NULL OR r.created_at >= :fromDateTime)
              AND (:toDateTime IS NULL OR r.created_at < :toDateTime)
            """,
            nativeQuery = true)
    Page<AdminClientReviewListRowProjection> searchReviewPage(@Param("memberId") Long memberId,
                                                              @Param("fromDateTime") LocalDateTime fromDateTime,
                                                              @Param("toDateTime") LocalDateTime toDateTime,
                                                              Pageable pageable);

    @Query("""
            select distinct r
            from ProductReview r
            left join fetch r.images i
            where r.memberId = :memberId
              and r.id in :reviewIds
            """)
    List<ProductReview> findAllWithImagesByMemberIdAndIdIn(@Param("memberId") Long memberId,
                                                           @Param("reviewIds") List<Long> reviewIds);
}