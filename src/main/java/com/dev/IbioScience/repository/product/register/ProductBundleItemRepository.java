package com.dev.IbioScience.repository.product.register;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.model.product.ProductBundleItem;

public interface ProductBundleItemRepository extends JpaRepository<ProductBundleItem, Long> {
	@Query("select b from ProductBundleItem b " +
           "join fetch b.bundleProduct p " +
           "where b.mainProduct.id = :pid order by b.sortOrder asc, b.id asc")
    List<ProductBundleItem> findByMainProductWithProduct(@Param("pid") Long productId);
}

