package com.dev.IbioScience.repository.product.dealer.review;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.product.dealer.review.DealerProductReview;

public interface DealerProductReviewRepository extends JpaRepository<DealerProductReview, Long>, DealerProductReviewRepositoryCustom  {

    boolean existsByDealerProductIdAndMemberId(Long dealerProductId, Long memberId);

    Optional<DealerProductReview> findByIdAndDealerProductId(Long reviewId, Long dealerProductId);

    @EntityGraph(attributePaths = {"images"})
    List<DealerProductReview> findByDealerProductIdOrderByCreatedAtDesc(Long dealerProductId);
    

}