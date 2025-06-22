package com.dev.IbioScience.repository.category;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;
import com.dev.IbioScience.model.product.relation.MediumSmallCategory;

public interface MediumSmallCategoryRepository extends JpaRepository<MediumSmallCategory, Long> {
    
	List<MediumSmallCategory> findBySmall(CategorySmall small);
    List<MediumSmallCategory> findByMedium(CategoryMedium medium);
    boolean existsByMedium(CategoryMedium medium);
    boolean existsBySmall(CategorySmall small);
    boolean existsByMediumAndSmall(CategoryMedium medium, CategorySmall small);
    void deleteBySmallAndMedium(CategorySmall small, CategoryMedium medium);
}