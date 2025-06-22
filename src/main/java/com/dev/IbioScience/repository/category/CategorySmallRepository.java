package com.dev.IbioScience.repository.category;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dev.IbioScience.model.product.category.CategorySmall;

@Repository
public interface CategorySmallRepository extends JpaRepository<CategorySmall, Long> {
    Optional<CategorySmall> findByName(String name);
    boolean existsByName(String name);
}