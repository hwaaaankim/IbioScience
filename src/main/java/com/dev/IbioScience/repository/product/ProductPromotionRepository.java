package com.dev.IbioScience.repository.product;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.model.product.Promotion;
import com.dev.IbioScience.model.product.enums.PromotionType;

public interface ProductPromotionRepository extends JpaRepository<Promotion, Long> {
	@Query("""
	    SELECT p FROM Promotion p
	    WHERE (:name IS NULL OR p.name LIKE %:name%)
	      AND (:type IS NULL OR p.type = :type)
	      AND (:startDate IS NULL OR p.startDate >= :startDate)
	      AND (:endDate IS NULL OR p.endDate <= :endDate)
	      AND (:active IS NULL OR p.active = :active)
	    ORDER BY p.id DESC
	""")
    List<Promotion> findBySearchConditions(
        @Param("name") String name,
        @Param("type") PromotionType type,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("active") Boolean active
    );
}