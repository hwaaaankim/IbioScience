package com.dev.IbioScience.repository.category;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dev.IbioScience.model.product.category.CategoryLarge;

@Repository
public interface CategoryLargeRepository extends JpaRepository<CategoryLarge, Long> {
    Optional<CategoryLarge> findByName(String name);
    boolean existsByName(String name);
    boolean existsById(Long id);
}

