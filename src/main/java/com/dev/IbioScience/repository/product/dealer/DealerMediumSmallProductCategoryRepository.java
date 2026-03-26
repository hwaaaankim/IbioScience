package com.dev.IbioScience.repository.product.dealer;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.ProductState;
import com.dev.IbioScience.enums.product.SaleStatus;
import com.dev.IbioScience.model.product.dealer.DealerMediumSmallProductCategory;
import com.dev.IbioScience.model.product.dealer.DealerProduct;

public interface DealerMediumSmallProductCategoryRepository
        extends JpaRepository<DealerMediumSmallProductCategory, Long> {

    @Query("""
        select distinct dmspc.dealerProduct
          from DealerMediumSmallProductCategory dmspc
          join dmspc.dealerProduct p
         where dmspc.medium.id = :mediumId
           and dmspc.small.id = :smallId
           and p.displayStatus = :displayStatus
           and p.saleStatus = :saleStatus
           and p.state = :state
         order by p.name asc, p.id desc
    """)
    List<DealerProduct> findProductsByMediumAndSmall(@Param("mediumId") Long mediumId,
                                                     @Param("smallId") Long smallId,
                                                     @Param("displayStatus") DisplayStatus displayStatus,
                                                     @Param("saleStatus") SaleStatus saleStatus,
                                                     @Param("state") ProductState state);

    @Query("""
        select distinct dmspc.dealerProduct
          from DealerMediumSmallProductCategory dmspc
          join dmspc.dealerProduct p
         where dmspc.medium.id = :mediumId
           and p.displayStatus = :displayStatus
           and p.saleStatus = :saleStatus
           and p.state = :state
         order by p.name asc, p.id desc
    """)
    List<DealerProduct> findProductsByMedium(@Param("mediumId") Long mediumId,
                                             @Param("displayStatus") DisplayStatus displayStatus,
                                             @Param("saleStatus") SaleStatus saleStatus,
                                             @Param("state") ProductState state);

    @Query("""
        select distinct dmspc.dealerProduct
          from DealerMediumSmallProductCategory dmspc
          join dmspc.dealerProduct p
         where dmspc.medium.id in :mediumIds
           and p.displayStatus = :displayStatus
           and p.saleStatus = :saleStatus
           and p.state = :state
         order by p.name asc, p.id desc
    """)
    List<DealerProduct> findProductsByMediumIds(@Param("mediumIds") List<Long> mediumIds,
                                                @Param("displayStatus") DisplayStatus displayStatus,
                                                @Param("saleStatus") SaleStatus saleStatus,
                                                @Param("state") ProductState state);

    @Query("""
        select distinct dmspc.dealerProduct
          from DealerMediumSmallProductCategory dmspc
          join dmspc.dealerProduct p
         where dmspc.small.id = :smallId
           and p.displayStatus = :displayStatus
           and p.saleStatus = :saleStatus
           and p.state = :state
         order by p.name asc, p.id desc
    """)
    List<DealerProduct> findProductsBySmall(@Param("smallId") Long smallId,
                                            @Param("displayStatus") DisplayStatus displayStatus,
                                            @Param("saleStatus") SaleStatus saleStatus,
                                            @Param("state") ProductState state);
    
    
    @Query("""
            select distinct cm
            from DealerMediumSmallProductCategory cm
            join fetch cm.medium m
            join fetch m.large l
            join fetch cm.small s
            join cm.dealerProduct dp
            where dp.sellerDealerProfile.id = :sellerDealerProfileId
              and dp.state = :state
              and exists (
                    select 1
                    from DealerCategoryPermission dcp
                    where dcp.sellerDealerProfile.id = :sellerDealerProfileId
                      and dcp.large.id = l.id
                      and (dcp.medium is null or dcp.medium.id = m.id)
                      and (dcp.small is null or dcp.small.id = s.id)
              )
            order by l.name asc, m.name asc, s.name asc
            """)
    List<DealerMediumSmallProductCategory> findVisibleMappingsForSeller(
            @Param("sellerDealerProfileId") Long sellerDealerProfileId,
            @Param("state") ProductState state
    );

    @Query("""
            select distinct cm
            from DealerMediumSmallProductCategory cm
            join fetch cm.medium m
            join fetch m.large l
            join fetch cm.small s
            where cm.dealerProduct.id in :dealerProductIds
            order by l.name asc, m.name asc, s.name asc
            """)
    List<DealerMediumSmallProductCategory> findAllByDealerProductIdsWithCategory(
            @Param("dealerProductIds") List<Long> dealerProductIds
    );
}