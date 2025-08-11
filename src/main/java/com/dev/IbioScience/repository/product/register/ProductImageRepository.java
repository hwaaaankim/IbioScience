package com.dev.IbioScience.repository.product.register;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.model.product.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
	
	@Query("select i from ProductImage i where i.product.id = :pid order by i.type asc, i.sortOrder asc, i.id asc")
    List<ProductImage> findAllByProductOrder(@Param("pid") Long productId);
}

