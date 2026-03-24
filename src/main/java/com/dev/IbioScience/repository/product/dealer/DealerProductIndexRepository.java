package com.dev.IbioScience.repository.product.dealer;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.ProductImageType;
import com.dev.IbioScience.enums.product.ProductState;
import com.dev.IbioScience.enums.product.SaleStatus;
import com.dev.IbioScience.model.product.dealer.DealerProduct;

public interface DealerProductIndexRepository extends JpaRepository<DealerProduct, Long> {

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
}