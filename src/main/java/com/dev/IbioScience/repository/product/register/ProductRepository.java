package com.dev.IbioScience.repository.product.register;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.dto.productRegister.ProductSimpleDTO;
import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.ProductState;
import com.dev.IbioScience.enums.product.SaleStatus;
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
    
    // 제품 리스트 PAGE
    /**
     * 기본 정렬(등록일/이름/가격 등)용 검색
     */
    @Query("""
        select distinct p
        from Product p
        left join SmallProductCategory spc on spc.product = p
        left join CategorySmall cs on spc.small = cs
        left join MediumSmallCategory msc on msc.small = cs
        left join CategoryMedium cm on msc.medium = cm
        left join CategoryLarge cl on cm.large = cl
        where (:brandId is null or p.brand.id = :brandId)
        and (:smallId is null or cs.id = :smallId)
        and (:mediumId is null or cm.id = :mediumId)
        and (:largeId is null or cl.id = :largeId)
        and (:keyword is null
             or lower(p.name) like lower(concat('%', :keyword, '%'))
             or lower(p.summaryDescription) like lower(concat('%', :keyword, '%'))
             or lower(p.shortDescription) like lower(concat('%', :keyword, '%'))
        )
        and (:displayStatus is null or p.displayStatus = :displayStatus)
        and (:saleStatus is null or p.saleStatus = :saleStatus)
        and (:state is null or p.state = :state)
    """)
    Page<Product> searchProducts(
            @Param("largeId") Long largeId,
            @Param("mediumId") Long mediumId,
            @Param("smallId") Long smallId,
            @Param("brandId") Long brandId,
            @Param("keyword") String keyword,
            @Param("displayStatus") DisplayStatus displayStatus,
            @Param("saleStatus") SaleStatus saleStatus,
            @Param("state") ProductState state,
            Pageable pageable
    );

    /**
     * 별점 높은 순
     */
    @Query("""
        select p
        from Product p
        left join ProductReview r on r.product = p
        left join SmallProductCategory spc on spc.product = p
        left join CategorySmall cs on spc.small = cs
        left join MediumSmallCategory msc on msc.small = cs
        left join CategoryMedium cm on msc.medium = cm
        left join CategoryLarge cl on cm.large = cl
        where (:brandId is null or p.brand.id = :brandId)
        and (:smallId is null or cs.id = :smallId)
        and (:mediumId is null or cm.id = :mediumId)
        and (:largeId is null or cl.id = :largeId)
        and (:keyword is null
             or lower(p.name) like lower(concat('%', :keyword, '%'))
             or lower(p.summaryDescription) like lower(concat('%', :keyword, '%'))
             or lower(p.shortDescription) like lower(concat('%', :keyword, '%'))
        )
        and (:displayStatus is null or p.displayStatus = :displayStatus)
        and (:saleStatus is null or p.saleStatus = :saleStatus)
        and (:state is null or p.state = :state)
        group by p
        order by avg(r.rating) desc nulls last
    """)
    Page<Product> searchProductsOrderByRatingDesc(
            @Param("largeId") Long largeId,
            @Param("mediumId") Long mediumId,
            @Param("smallId") Long smallId,
            @Param("brandId") Long brandId,
            @Param("keyword") String keyword,
            @Param("displayStatus") DisplayStatus displayStatus,
            @Param("saleStatus") SaleStatus saleStatus,
            @Param("state") ProductState state,
            Pageable pageable
    );

    /**
     * 별점 낮은 순
     */
    @Query("""
        select p
        from Product p
        left join ProductReview r on r.product = p
        left join SmallProductCategory spc on spc.product = p
        left join CategorySmall cs on spc.small = cs
        left join MediumSmallCategory msc on msc.small = cs
        left join CategoryMedium cm on msc.medium = cm
        left join CategoryLarge cl on cm.large = cl
        where (:brandId is null or p.brand.id = :brandId)
        and (:smallId is null or cs.id = :smallId)
        and (:mediumId is null or cm.id = :mediumId)
        and (:largeId is null or cl.id = :largeId)
        and (:keyword is null
             or lower(p.name) like lower(concat('%', :keyword, '%'))
             or lower(p.summaryDescription) like lower(concat('%', :keyword, '%'))
             or lower(p.shortDescription) like lower(concat('%', :keyword, '%'))
        )
        and (:displayStatus is null or p.displayStatus = :displayStatus)
        and (:saleStatus is null or p.saleStatus = :saleStatus)
        and (:state is null or p.state = :state)
        group by p
        order by avg(r.rating) asc nulls last
    """)
    Page<Product> searchProductsOrderByRatingAsc(
            @Param("largeId") Long largeId,
            @Param("mediumId") Long mediumId,
            @Param("smallId") Long smallId,
            @Param("brandId") Long brandId,
            @Param("keyword") String keyword,
            @Param("displayStatus") DisplayStatus displayStatus,
            @Param("saleStatus") SaleStatus saleStatus,
            @Param("state") ProductState state,
            Pageable pageable
    );
}

