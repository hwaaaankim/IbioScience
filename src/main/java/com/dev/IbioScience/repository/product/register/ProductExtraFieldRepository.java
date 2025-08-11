package com.dev.IbioScience.repository.product.register;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.product.ProductExtraField;

public interface ProductExtraFieldRepository extends JpaRepository<ProductExtraField, Long> {
	List<ProductExtraField> findByProductIdOrderByIdAsc(Long productId);
	List<ProductExtraField> findByProductId(Long productId);
}

