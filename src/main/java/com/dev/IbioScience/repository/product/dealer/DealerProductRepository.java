package com.dev.IbioScience.repository.product.dealer;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.model.product.dealer.DealerProduct;

public interface DealerProductRepository extends JpaRepository<DealerProduct, Long> {

    boolean existsBySellerDealerProfileIdAndCode(Long sellerDealerProfileId, String code);

    boolean existsBySellerDealerProfileIdAndCodeAndIdNot(Long sellerDealerProfileId, String code, Long id);

    @Query("""
            select dp
            from DealerProduct dp
            join fetch dp.sellerDealerProfile sdp
            join fetch sdp.member m
            where dp.id = :dealerProductId
              and m.id = :sellerMemberId
            """)
    Optional<DealerProduct> findOwnedByIdAndSellerMemberId(
            @Param("dealerProductId") Long dealerProductId,
            @Param("sellerMemberId") Long sellerMemberId
    );
}	