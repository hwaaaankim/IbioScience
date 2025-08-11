package com.dev.IbioScience.repository.product;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.dev.IbioScience.model.product.InternalCategoryMedium;
import com.dev.IbioScience.model.product.InternalCategorySmall;

public interface InternalCategorySmallRepository extends JpaRepository<InternalCategorySmall, Long> {
    // 중분류, 소분류명으로 중복 체크
    boolean existsByMediumAndName(InternalCategoryMedium medium, String name);

    // 중분류 기준, id 오름차순 전체 조회
    List<InternalCategorySmall> findAllByMediumOrderByIdAsc(InternalCategoryMedium medium);
    
    @Query("""
        select s.id as id, s.name as name, count(p.id) as productCount
        from InternalCategorySmall s
        left join s.products p
        where s.medium.id = :mediumId
        group by s.id, s.name
        order by s.name asc
    """)
    List<Object[]> findByMediumIdWithProductCount(Long mediumId);
}