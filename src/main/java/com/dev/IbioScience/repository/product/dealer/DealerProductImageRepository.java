package com.dev.IbioScience.repository.product.dealer;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.enums.product.ProductImageType;
import com.dev.IbioScience.model.product.dealer.DealerProductImage;

public interface DealerProductImageRepository extends JpaRepository<DealerProductImage, Long> {

    @Query("""
            select dpi
            from DealerProductImage dpi
            join fetch dpi.dealerProduct dp
            where dp.id in :dealerProductIds
              and dpi.type = :type
            order by dp.id asc, dpi.sortOrder asc, dpi.id asc
            """)
    List<DealerProductImage> findMainImagesByDealerProductIds(
            @Param("dealerProductIds") List<Long> dealerProductIds,
            @Param("type") ProductImageType type
    );
    
    @Query(
        value = """
            select *
            from tb_dealer_product_image img
            where img.dealer_product_id in (:dealerProductIds)
              and img.type = 'MAIN'
            order by img.dealer_product_id asc,
                     case when img.sort_order is null then 2147483647 else img.sort_order end asc,
                     img.id asc
            """,
        nativeQuery = true
    )
    List<DealerProductImage> findMainImagesByDealerProductIds(
            @Param("dealerProductIds") List<Long> dealerProductIds
    );
}