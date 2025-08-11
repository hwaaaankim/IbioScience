package com.dev.IbioScience.repository.product.register;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.dev.IbioScience.model.product.Product;

// Product
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
	// 브랜드에 연결된 제품이 하나라도 존재하는지 여부
    boolean existsByBrand_Id(Long brandId);
    boolean existsByCode(String code);
}

