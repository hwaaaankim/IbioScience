package com.dev.IbioScience.repository.product.register;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.product.Product;

// Product
public interface ProductRepository extends JpaRepository<Product, Long> {}

