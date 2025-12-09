package com.dev.IbioScience.repository.product.review;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.product.review.ProductReviewImage;

public interface ProductReviewImageRepository extends JpaRepository<ProductReviewImage, Long> {
	
	 /**
     * 리뷰에 연결된 이미지 전체 조회
     */
    List<ProductReviewImage> findByReviewId(Long reviewId);

    /**
     * 리뷰에 연결된 이미지 전체 삭제 (DB 기준)
     * - 서비스에서 파일 삭제 후 호출하면 편하게 사용 가능
     */
    void deleteByReview_Id(Long reviewId);
}