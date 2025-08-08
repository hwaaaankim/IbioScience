package com.dev.IbioScience.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.product.relation.ProductPromotionMapping;

public interface ProductPromotionMappingRepository extends JpaRepository<ProductPromotionMapping, Long> {
    long countByPromotion_Id(Long promotionId);
}