package com.dev.IbioScience.repository.category;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;
import com.dev.IbioScience.model.product.relation.MediumSmallCategory;

public interface MediumSmallCategoryRepository extends JpaRepository<MediumSmallCategory, Long> {
    
	boolean existsByMediumIdAndSmallId(Long mediumId, Long smallId);
	
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
    
    @Query("select msc from MediumSmallCategory msc where msc.small.id in :smallIds")
    List<MediumSmallCategory> findBySmallIds(@Param("smallIds") Collection<Long> smallIds);
    
    @Query("select msc from MediumSmallCategory msc " +
            "join fetch msc.medium m " +
            "join fetch m.large l " +
            "where msc.small.id = :smallId " +
            "order by coalesce(msc.sortOrder, 0) asc, m.id asc")
     List<MediumSmallCategory> findPathsBySmall(@Param("smallId") Long smallId);
    
    List<MediumSmallCategory> findByMedium_IdOrderBySortOrderAsc(Long mediumId);
    
    /** 특정 중분류에 연결된 소분류들(정렬 우선: 매핑 sortOrder, 보조: 소분류명) */
    @Query("select msc.small from MediumSmallCategory msc " +
           "join msc.small s " +
           "where msc.medium.id = :mediumId " +
           "order by coalesce(msc.sortOrder, 999999), s.name asc")
    List<CategorySmall> findSmallsByMediumId(@Param("mediumId") Long mediumId);

    /** 특정 대분류의 모든 중분류에 연결된 소분류 ID 목록 */
    @Query("select distinct msc.small.id from MediumSmallCategory msc " +
           "where msc.medium.large.id = :largeId")
    List<Long> findSmallIdsByLargeId(@Param("largeId") Long largeId);

    /** 다수 중분류 -> 연결 소분류 ID들 */
    @Query("select distinct msc.small.id from MediumSmallCategory msc " +
           "where msc.medium.id in :mediumIds")
    List<Long> findSmallIdsByMediumIds(@Param("mediumIds") List<Long> mediumIds);
    
    List<MediumSmallCategory> findBySmallIn(Collection<CategorySmall> smalls);
    
    @Query("""
        select msc
        from MediumSmallCategory msc
        join fetch msc.medium m
        join fetch msc.small s
        where m.id in :mediumIds
        order by m.id asc, msc.sortOrder asc, msc.id asc
    """)
    List<MediumSmallCategory> findAllByMediumIds(@Param("mediumIds") Collection<Long> mediumIds);
    
    List<MediumSmallCategory> findByMediumId(Long mediumId);

}