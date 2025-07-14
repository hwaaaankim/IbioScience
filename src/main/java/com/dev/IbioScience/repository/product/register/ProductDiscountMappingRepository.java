package com.dev.IbioScience.repository.product.register;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.product.relation.ProductPromotionMapping;

public interface ProductDiscountMappingRepository extends JpaRepository<ProductPromotionMapping, Long> {}


