package com.dev.IbioScience.repository.product.register;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.model.product.ProductKeyword;

public interface ProductKeywordRepository extends JpaRepository<ProductKeyword, Long> {
	
	@Query("select pk from ProductKeyword pk join fetch pk.keyword k where pk.product.id = :pid order by pk.id asc")
    List<ProductKeyword> findByProductWithKeyword(@Param("pid") Long productId);
}
