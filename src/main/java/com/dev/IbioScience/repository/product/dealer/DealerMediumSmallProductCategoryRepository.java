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
}