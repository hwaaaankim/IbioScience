package com.dev.IbioScience.repository.category;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dev.IbioScience.model.product.category.CategoryLarge;
import com.dev.IbioScience.model.product.category.CategoryMedium;

@Repository
public interface CategoryMediumRepository extends JpaRepository<CategoryMedium, Long> {
    
	boolean existsByNameAndLarge(String name, CategoryLarge large);
    List<CategoryMedium> findByLarge(CategoryLarge large);
    List<CategoryMedium> findByLargeId(Long largeId);
    Optional<CategoryMedium> findByIdAndLargeId(Long id, Long largeId);
    List<CategoryMedium> findByLargeIdOrderByNameAsc(Long largeId);
    
    int countByLargeId(Long largeId);
    @Query("SELECT m.large.id, COUNT(m) FROM CategoryMedium m WHERE m.large.id IN :largeIds GROUP BY m.large.id")
    List<Object[]> countByLargeIds(@Param("largeIds") List<Long> largeIds);

}

