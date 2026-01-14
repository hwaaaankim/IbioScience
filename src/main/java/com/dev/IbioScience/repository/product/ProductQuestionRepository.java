package com.dev.IbioScience.repository.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.dev.IbioScience.model.product.ProductQuestion;

@Repository
public interface ProductQuestionRepository extends JpaRepository<ProductQuestion, Long> {
    List<ProductQuestion> findAllByOrderBySortOrderAsc();
    Optional<ProductQuestion> findByLabel(String label);
    
    /**
     * 공통표시사항(= required=true) 질문 목록 + 옵션 로딩
     * - options는 같이 fetch (질문 1개당 options 1 bag 이므로 보통 문제 없음)
     */
    @EntityGraph(attributePaths = { "options" })
    @Query("""
        select distinct q
        from ProductQuestion q
        where q.required = true
        order by q.sortOrder asc, q.id asc
    """)
    List<ProductQuestion> findRequiredWithOptionsOrder();
    
    List<ProductQuestion> findByRequiredTrueOrderBySortOrderAsc();
    
}