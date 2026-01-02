package com.dev.IbioScience.repository.product.register;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.model.product.ProductOptionGroup;

public interface ProductOptionGroupRepository extends JpaRepository<ProductOptionGroup, Long> {
	
	@Query("select distinct g from ProductOptionGroup g " +
           "left join fetch g.options o " +
           "where g.product.id = :pid " +
           "order by g.sortOrder asc, g.id asc, o.sortOrder asc, o.id asc")
    List<ProductOptionGroup> findWithOptions(@Param("pid") Long productId);
	
	@Query("""
        select distinct g
        from ProductOptionGroup g
        left join fetch g.options o
        where g.product.id in :productIds
        """)
    List<ProductOptionGroup> findWithOptionsByProductIds(@Param("productIds") List<Long> productIds);
}

