package com.dev.IbioScience.repository.category;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.dto.page.index.ProductSimpleDTO;
import com.dev.IbioScience.model.product.relation.MediumSmallProductCategory;

public interface MediumSmallProductCategoryRepository extends JpaRepository<MediumSmallProductCategory, Long> {

	boolean existsByMedium_IdAndSmall_Id(Long mediumId, Long smallId);
	
    @Query("select mspc from MediumSmallProductCategory mspc where mspc.product.id in :ids")
    List<MediumSmallProductCategory> findByProductIds(@Param("ids") Collection<Long> productIds);

    @Query("select count(mspc) from MediumSmallProductCategory mspc where mspc.product.id = :productId")
    long countByProductId(@Param("productId") Long productId);

    void deleteByProduct_Id(Long productId);
    
    boolean existsByProductIdAndMediumIdAndSmallId(Long productId, Long mediumId, Long smallId);
    
    @Query("""
        select distinct mspc
        from MediumSmallProductCategory mspc
        join fetch mspc.medium m
        join fetch mspc.small s
        join fetch m.large l
        where mspc.product.id = :productId
        order by l.id asc, m.id asc, s.id asc, mspc.id asc
    """)
    List<MediumSmallProductCategory> findByProductWithPath(@Param("productId") Long productId);
    
    @Query("""
        select mspc
        from MediumSmallProductCategory mspc
        join fetch mspc.medium
        join fetch mspc.small
        where mspc.product.id = :productId
    """)
    List<MediumSmallProductCategory> findAllByProductIdWithMediumSmall(@Param("productId") Long productId);
    
    @EntityGraph(attributePaths = { "medium", "medium.large", "small", "product" })
    List<MediumSmallProductCategory> findByProduct_IdIn(Collection<Long> productIds);
    
    // medium + small (가장 정확한 케이스: 소분류 클릭)
    @Query("""
	    select distinct new com.dev.IbioScience.dto.page.index.ProductSimpleDTO(
	        p.id,
	        p.name,
	        (case when b is null then null else b.id end)
	    )
	    from MediumSmallProductCategory mspc
	    join mspc.product p
	    left join p.brand b
	    where mspc.medium.id = :mediumId
	      and mspc.small.id  = :smallId
	      and p.displayStatus = 'ON'
	      and (:brandId is null or b.id = :brandId)
	    order by p.name asc
	""")
	List<ProductSimpleDTO> findProductsByMediumAndSmall(
	        @Param("mediumId") Long mediumId,
	        @Param("smallId") Long smallId,
	        @Param("brandId") Long brandId
	);


    // medium 전체(중분류 클릭)
    @Query("""
	    select distinct new com.dev.IbioScience.dto.page.index.ProductSimpleDTO(
	        p.id,
	        p.name,
	        (case when b is null then null else b.id end)
	    )
	    from MediumSmallProductCategory mspc
	    join mspc.product p
	    left join p.brand b
	    where mspc.medium.id = :mediumId
	      and p.displayStatus = 'ON'
	      and (:brandId is null or b.id = :brandId)
	    order by p.name asc
	""")
	List<ProductSimpleDTO> findProductsByMedium(
	        @Param("mediumId") Long mediumId,
	        @Param("brandId") Long brandId
	);


    // large 클릭(= large 하위 mediumIds 전체)
    @Query("""
	    select distinct new com.dev.IbioScience.dto.page.index.ProductSimpleDTO(
	        p.id,
	        p.name,
	        (case when b is null then null else b.id end)
	    )
	    from MediumSmallProductCategory mspc
	    join mspc.product p
	    left join p.brand b
	    where mspc.medium.id in :mediumIds
	      and p.displayStatus = 'ON'
	      and (:brandId is null or b.id = :brandId)
	    order by p.name asc
	""")
	List<ProductSimpleDTO> findProductsByMediumIds(
	        @Param("mediumIds") Collection<Long> mediumIds,
	        @Param("brandId") Long brandId
	);


    // (호환) smallId만 들어온 케이스
    @Query("""
	    select distinct new com.dev.IbioScience.dto.page.index.ProductSimpleDTO(
	        p.id,
	        p.name,
	        (case when b is null then null else b.id end)
	    )
	    from MediumSmallProductCategory mspc
	    join mspc.product p
	    left join p.brand b
	    where mspc.small.id = :smallId
	      and p.displayStatus = 'ON'
	      and (:brandId is null or b.id = :brandId)
	    order by p.name asc
	""")
	List<ProductSimpleDTO> findProductsBySmall(
	        @Param("smallId") Long smallId,
	        @Param("brandId") Long brandId
	);
    
    List<MediumSmallProductCategory> findAllByIdIn(Collection<Long> ids);

    List<MediumSmallProductCategory> findByProduct_IdOrderByIdAsc(Long productId);

}