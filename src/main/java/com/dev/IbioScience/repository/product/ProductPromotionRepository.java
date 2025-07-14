package com.dev.IbioScience.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.product.Promotion;

public interface ProductPromotionRepository extends JpaRepository<Promotion, Long> {
}