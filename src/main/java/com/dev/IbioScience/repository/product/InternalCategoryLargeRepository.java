package com.dev.IbioScience.repository.product;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.dev.IbioScience.model.product.InternalCategoryLarge;

public interface InternalCategoryLargeRepository extends JpaRepository<InternalCategoryLarge, Long> {
    // 대분류명 중복 체크
    boolean existsByName(String name);

    // 전체 id 오름차순 조회
    List<InternalCategoryLarge> findAllByOrderByIdAsc();
    
    @Query("""
        select l.id as id, l.name as name, count(m.id) as mediumCount
        from InternalCategoryLarge l
        left join l.mediums m
        group by l.id, l.name
        order by l.name asc
    """)
    List<Object[]> findAllWithMediumCount();
}