package com.dev.IbioScience.repository.product.register;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.enums.product.RelatedType;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.RelatedProduct;

public interface RelatedProductRepository extends JpaRepository<RelatedProduct, Long> {
	
	boolean existsByBaseProductAndRelatedProductAndType(Product baseProduct, Product relatedProduct, RelatedType type);
	@Query("select r from RelatedProduct r join fetch r.relatedProduct p " +
           "where r.baseProduct.id = :pid order by r.sortOrder asc, r.id asc")
    List<RelatedProduct> findByBaseProductWithProduct(@Param("pid") Long productId);
}

