package com.dev.IbioScience.repository.product.review;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.dto.page.productList.ProductRatingSummaryDto;
import com.dev.IbioScience.model.product.review.ProductReview;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

	Page<ProductReview> findByProduct_Id(Long productId, Pageable pageable);

	boolean existsByProduct_IdAndMemberId(Long productId, Long memberId);

	@Query("select coalesce(avg(r.rating),0) from ProductReview r where r.product.id = :productId")
	Double getAverageRating(Long productId);

	@Query("select count(r.id) from ProductReview r where r.product.id = :productId")
	Long getReviewCount(Long productId);
	
	@Query("""
        select new com.dev.IbioScience.dto.page.productList.ProductRatingSummaryDto(
            r.product.id,
            avg(r.rating),
            count(r)
        )
        from ProductReview r
        where r.product.id in :productIds
        group by r.product.id
    """)
    List<ProductRatingSummaryDto> findRatingSummaryByProductIds(
            @Param("productIds") Collection<Long> productIds
    );
	
	@Query("""
        select 
            avg(r.rating) as averageRating,
            count(r)      as reviewCount
        from ProductReview r
        where r.product.id = :productId
        """)
    ProductReviewSummaryProjection findSummaryByProductId(@Param("productId") Long productId);

    List<ProductReview> findTop5ByProductIdOrderByCreatedAtDesc(Long productId);

    interface ProductReviewSummaryProjection {
        Double getAverageRating();
        Long getReviewCount();
    }
}