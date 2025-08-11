package com.dev.IbioScience.repository.category;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.relation.SmallProductCategory;

public interface SmallProductCategoryRepository extends JpaRepository<SmallProductCategory, Long> {
   
	@Query("SELECT COUNT(spc) FROM SmallProductCategory spc WHERE spc.small.id = :smallId")
    int countBySmallId(@Param("smallId") Long smallId);
	
	@Query("SELECT p FROM SmallProductCategory spc JOIN spc.product p WHERE spc.small.id = :smallId")
    List<Product> findProductsBySmallCategoryId(@Param("smallId") Long smallId);
	
	@Query("select spc from SmallProductCategory spc where spc.product.id in :ids")
    List<SmallProductCategory> findByProductIds(@Param("ids") Collection<Long> productIds);
	
	@Query("select spc from SmallProductCategory spc join fetch spc.small s where spc.product.id = :pid")
    List<SmallProductCategory> findByProductWithSmall(@Param("pid") Long productId);
}