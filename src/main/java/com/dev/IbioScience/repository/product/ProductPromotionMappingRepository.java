package com.dev.IbioScience.repository.product;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.Promotion;
import com.dev.IbioScience.model.product.relation.ProductPromotionMapping;

public interface ProductPromotionMappingRepository extends JpaRepository<ProductPromotionMapping, Long> {
    long countByPromotion_Id(Long promotionId);
    boolean existsByProductAndPromotion(Product product, Promotion promotion);
    
    @Query("""
        select ppm.product.id as productId, p.type as type
        from ProductPromotionMapping ppm
          join ppm.promotion p
        where ppm.product.id in :productIds
          and (:today is null or :today = :today)  
        """)
    List<Object[]> findActivePromotionTypesByProductIds(
            @Param("productIds") Collection<Long> productIds,
            @Param("today") LocalDate today
    );

    // (참고용) 전체 매핑에서 (productId, type) 집합
    @Query(
        "select distinct ppm.product.id, ppm.promotion.type " +
        "from ProductPromotionMapping ppm " +
        "where ppm.product.id in :ids"
    )
    List<Object[]> findAllPromotionTypesByProductIds(@Param("ids") Collection<Long> ids);
    @Query("select m from ProductPromotionMapping m " +
           "join fetch m.promotion pr " +
           "where m.product.id = :pid")
    List<ProductPromotionMapping> findByProductWithPromotion(@Param("pid") Long productId);
}