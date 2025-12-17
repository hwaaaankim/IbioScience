package com.dev.IbioScience.repository.category;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.model.product.relation.MediumSmallProductCategory;

public interface MediumSmallProductCategoryRepository extends JpaRepository<MediumSmallProductCategory, Long> {

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
}