package com.dev.IbioScience.repository.product.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.dev.IbioScience.model.product.review.ProductReview;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

	Page<ProductReview> findByProduct_Id(Long productId, Pageable pageable);

	boolean existsByProduct_IdAndMemberId(Long productId, Long memberId);

	@Query("select coalesce(avg(r.rating),0) from ProductReview r where r.product.id = :productId")
	Double getAverageRating(Long productId);

	@Query("select count(r.id) from ProductReview r where r.product.id = :productId")
	Long getReviewCount(Long productId);
}