package com.dev.IbioScience.repository.product.dealer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.ProductImageType;
import com.dev.IbioScience.enums.product.ProductState;
import com.dev.IbioScience.enums.product.SaleStatus;
import com.dev.IbioScience.model.product.dealer.DealerProduct;

public interface DealerProductRepository extends JpaRepository<DealerProduct, Long>, DealerProductRepositoryCustom {

	 @Query(
        value = """
            select dp.id
            from tb_dealer_product dp
            where dp.state = 'NORMAL'
              and dp.display_status = 'ON'
              and dp.sale_status = 'ON'
            order by dp.name asc, dp.id asc
            """,
        countQuery = """
            select count(*)
            from tb_dealer_product dp
            where dp.state = 'NORMAL'
              and dp.display_status = 'ON'
              and dp.sale_status = 'ON'
            """,
        nativeQuery = true
    )
    Page<Long> findFrontActiveIdsOrderByNameAsc(Pageable pageable);

    @Query(
        value = """
            select dp.id
            from tb_dealer_product dp
            where dp.state = 'NORMAL'
              and dp.display_status = 'ON'
              and dp.sale_status = 'ON'
            order by dp.name desc, dp.id desc
            """,
        countQuery = """
            select count(*)
            from tb_dealer_product dp
            where dp.state = 'NORMAL'
              and dp.display_status = 'ON'
              and dp.sale_status = 'ON'
            """,
        nativeQuery = true
    )
    Page<Long> findFrontActiveIdsOrderByNameDesc(Pageable pageable);

    @Query(
        value = """
            select dp.id
            from tb_dealer_product dp
            where dp.state = 'NORMAL'
              and dp.display_status = 'ON'
              and dp.sale_status = 'ON'
            order by
              case when dp.sale_price is null then 1 else 0 end asc,
              dp.sale_price asc,
              dp.id asc
            """,
        countQuery = """
            select count(*)
            from tb_dealer_product dp
            where dp.state = 'NORMAL'
              and dp.display_status = 'ON'
              and dp.sale_status = 'ON'
            """,
        nativeQuery = true
    )
    Page<Long> findFrontActiveIdsOrderByPriceAsc(Pageable pageable);

    @Query(
        value = """
            select dp.id
            from tb_dealer_product dp
            where dp.state = 'NORMAL'
              and dp.display_status = 'ON'
              and dp.sale_status = 'ON'
            order by
              case when dp.sale_price is null then 1 else 0 end asc,
              dp.sale_price desc,
              dp.id desc
            """,
        countQuery = """
            select count(*)
            from tb_dealer_product dp
            where dp.state = 'NORMAL'
              and dp.display_status = 'ON'
              and dp.sale_status = 'ON'
            """,
        nativeQuery = true
    )
    Page<Long> findFrontActiveIdsOrderByPriceDesc(Pageable pageable);

    @Query("""
        select distinct dp
        from DealerProduct dp
        join fetch dp.sellerDealerProfile sdp
        where dp.id in :dealerProductIds
        """)
    List<DealerProduct> findAllWithSellerByIdIn(@Param("dealerProductIds") List<Long> dealerProductIds);
	
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
    
    @Query("""
        select p.id,
               p.name,
               p.salePrice,
               p.consumerPrice,
               p.salesCount,
               p.viewCount,
               0.0,
               0,
               img.url,
               null,
               null
          from DealerProduct p
          left join p.images img on img.type = :mainType
         where p.displayStatus = :displayStatus
           and p.saleStatus = :saleStatus
           and p.state = :state
         order by p.viewCount desc, p.id desc
    """)
    List<Object[]> findTopViewedRaw(@Param("displayStatus") DisplayStatus displayStatus,
                                    @Param("saleStatus") SaleStatus saleStatus,
                                    @Param("state") ProductState state,
                                    @Param("mainType") ProductImageType mainType,
                                    Pageable pageable);

    @Query("""
        select p.id,
               p.name,
               p.salePrice,
               p.consumerPrice,
               p.salesCount,
               p.viewCount,
               0.0,
               0,
               img.url,
               null,
               null
          from DealerProduct p
          left join p.images img on img.type = :mainType
         where p.displayStatus = :displayStatus
           and p.saleStatus = :saleStatus
           and p.state = :state
         order by p.salesCount desc, p.id desc
    """)
    List<Object[]> findTopSalesRaw(@Param("displayStatus") DisplayStatus displayStatus,
                                   @Param("saleStatus") SaleStatus saleStatus,
                                   @Param("state") ProductState state,
                                   @Param("mainType") ProductImageType mainType,
                                   Pageable pageable);

    @Query(value = """
        select p.id,
               p.name,
               p.sale_price,
               p.consumer_price,
               p.sales_count,
               p.view_count,
               0.0,
               0,
               img.url,
               null,
               null
          from tb_dealer_product p
          left join tb_dealer_product_image img
            on img.dealer_product_id = p.id
           and img.type = 'MAIN'
         where p.display_status = :displayStatus
           and p.sale_status = :saleStatus
           and p.state = :state
         order by rand()
        """, nativeQuery = true)
    List<Object[]> findRandomRaw(@Param("displayStatus") String displayStatus,
                                 @Param("saleStatus") String saleStatus,
                                 @Param("state") String state,
                                 Pageable pageable);
    
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DealerProduct dp
               set dp.state = :waitingDeleteState,
                   dp.updatedAt = :updatedAt
             where dp.sellerDealerProfile.id = :sellerDealerProfileId
               and dp.id in :dealerProductIds
               and dp.state = :normalState
            """)
    int markWaitingDelete(
            @Param("sellerDealerProfileId") Long sellerDealerProfileId,
            @Param("dealerProductIds") List<Long> dealerProductIds,
            @Param("normalState") ProductState normalState,
            @Param("waitingDeleteState") ProductState waitingDeleteState,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}	