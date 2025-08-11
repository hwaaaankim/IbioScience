package com.dev.IbioScience.repository.product.register;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.dto.productRegister.ProductSimpleDTO;
import com.dev.IbioScience.model.product.Product;

// Product
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
	// 브랜드에 연결된 제품이 하나라도 존재하는지 여부
    boolean existsByBrand_Id(Long brandId);
    boolean existsByCode(String code);
    
    @Query("""
        SELECT DISTINCT new com.dev.IbioScience.dto.productRegister.ProductSimpleDTO(p.id, p.code, p.name)
        FROM Product p
            JOIN SmallProductCategory spc ON spc.product = p
            JOIN CategorySmall s ON spc.small = s
            LEFT JOIN MediumSmallCategory msc ON msc.small = s
            LEFT JOIN CategoryMedium m ON msc.medium = m
            LEFT JOIN CategoryLarge l ON m.large = l
        WHERE
            (:keyword IS NULL OR :keyword = '' OR
                LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            AND (:smallId  IS NULL OR s.id = :smallId)
            AND (:mediumId IS NULL OR m.id = :mediumId)
            AND (:largeId  IS NULL OR l.id = :largeId)
        ORDER BY p.id DESC
    """)
    List<ProductSimpleDTO> searchSimpleProducts(
            @Param("largeId")  Long largeId,
            @Param("mediumId") Long mediumId,
            @Param("smallId")  Long smallId,
            @Param("keyword")  String keyword,
            Pageable pageable
    );
    
    @EntityGraph(attributePaths = {
            "brand",
            "internalCategorySmall",
            "internalCategorySmall.medium",
            "internalCategorySmall.medium.large"
    })
    Optional<Product> findById(Long id);
}

