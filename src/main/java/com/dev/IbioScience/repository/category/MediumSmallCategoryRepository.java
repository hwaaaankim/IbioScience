package com.dev.IbioScience.repository.category;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;
import com.dev.IbioScience.model.product.relation.MediumSmallCategory;

public interface MediumSmallCategoryRepository extends JpaRepository<MediumSmallCategory, Long> {
    
	List<MediumSmallCategory> findBySmall(CategorySmall small);
    List<MediumSmallCategory> findByMedium(CategoryMedium medium);
    List<MediumSmallCategory> findAll();

    boolean existsByMedium(CategoryMedium medium);
    boolean existsBySmall(CategorySmall small);
    boolean existsByMediumAndSmall(CategoryMedium medium, CategorySmall small);
    void deleteBySmallAndMedium(CategorySmall small, CategoryMedium medium);
   
    @Query("SELECT m.small FROM MediumSmallCategory m WHERE m.medium.id = :mediumId ORDER BY m.sortOrder ASC, m.small.name ASC")
    List<CategorySmall> findSmallByMediumId(@Param("mediumId") Long mediumId);
    
    @Query("SELECT COUNT(msc) FROM MediumSmallCategory msc WHERE msc.medium.id = :mediumId")
    int countByMediumId(@Param("mediumId") Long mediumId);
    
    @Query("SELECT m.medium.id, COUNT(m) FROM MediumSmallCategory m WHERE m.medium.id IN :mediumIds GROUP BY m.medium.id")
    List<Object[]> countByMediumIds(@Param("mediumIds") List<Long> mediumIds);
}