package com.dev.IbioScience.repository.product.review;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.dto.page.productList.ProductRatingSummaryDto;
import com.dev.IbioScience.model.product.Product;
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
    
    boolean existsByProductAndMemberId(Product product, Long memberId);
    
    /**
     * 해당 상품에 대해 해당 회원이 이미 리뷰를 작성했는지 여부
     */
    boolean existsByProductIdAndMemberId(Long productId, Long memberId);

    /**
     * 특정 상품의 리뷰 목록 (최신순) – 필요 시 사용
     */
    List<ProductReview> findByProductIdOrderByCreatedAtDesc(Long productId);

    /**
     * 본인 리뷰인지 검증할 때 사용 가능 (선택)
     */
    Optional<ProductReview> findByIdAndMemberId(Long id, Long memberId);

    /**
     * 상품 + 회원 기준으로 리뷰 1개 조회 (중복 작성 방지용, 선택)
     */
    Optional<ProductReview> findByProductIdAndMemberId(Long productId, Long memberId);
}