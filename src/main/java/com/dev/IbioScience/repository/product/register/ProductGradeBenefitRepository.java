package com.dev.IbioScience.repository.product.register;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.model.product.ProductGradeBenefit;

public interface ProductGradeBenefitRepository extends JpaRepository<ProductGradeBenefit, Long> {
	// 페이징 결과의 productIds 에 대해 한 번에 등급혜택 조회
    @Query("select pgb from ProductGradeBenefit pgb where pgb.product.id in :ids")
    List<ProductGradeBenefit> findByProductIds(@Param("ids") Collection<Long> ids);
    
    List<ProductGradeBenefit> findByProductId(Long productId);
}
