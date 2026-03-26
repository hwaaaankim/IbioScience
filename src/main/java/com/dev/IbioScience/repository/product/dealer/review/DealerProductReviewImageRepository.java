package com.dev.IbioScience.repository.product.dealer.review;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.product.dealer.review.DealerProductReviewImage;

public interface DealerProductReviewImageRepository extends JpaRepository<DealerProductReviewImage, Long> {

    List<DealerProductReviewImage> findByReviewId(Long reviewId);
    
    List<DealerProductReviewImage> findByReviewIdOrderBySortOrderAscIdAsc(Long reviewId);

    List<DealerProductReviewImage> findByIdInAndReviewId(Collection<Long> ids, Long reviewId);
}