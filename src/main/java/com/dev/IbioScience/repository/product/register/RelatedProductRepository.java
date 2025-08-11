package com.dev.IbioScience.repository.product.register;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.RelatedProduct;
import com.dev.IbioScience.model.product.enums.RelatedType;

public interface RelatedProductRepository extends JpaRepository<RelatedProduct, Long> {
	
	boolean existsByBaseProductAndRelatedProductAndType(Product baseProduct, Product relatedProduct, RelatedType type);
}

