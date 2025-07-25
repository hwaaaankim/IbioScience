package com.dev.IbioScience.repository.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.product.Brand;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    // 브랜드명 부분 검색 + 페이징
    Page<Brand> findByNameContaining(String keyword, Pageable pageable);
}