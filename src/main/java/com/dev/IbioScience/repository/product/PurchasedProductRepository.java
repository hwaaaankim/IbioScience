package com.dev.IbioScience.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.relation.PurchasedProduct;

public interface PurchasedProductRepository extends JpaRepository<PurchasedProduct, Long> {

    boolean existsByMemberIdAndProduct(Long memberId, Product product);
}