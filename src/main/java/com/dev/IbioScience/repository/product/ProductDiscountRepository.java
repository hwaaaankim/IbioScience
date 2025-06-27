package com.dev.IbioScience.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.product.ProductDiscount;

public interface ProductDiscountRepository extends JpaRepository<ProductDiscount, Long> {
}