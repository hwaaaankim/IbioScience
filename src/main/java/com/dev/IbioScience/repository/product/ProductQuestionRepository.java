package com.dev.IbioScience.repository.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.dev.IbioScience.model.product.ProductQuestion;

@Repository
public interface ProductQuestionRepository extends JpaRepository<ProductQuestion, Long> {
    List<ProductQuestion> findAllByOrderBySortOrderAsc();
    Optional<ProductQuestion> findByLabel(String label);
    
    @Query("select distinct q from ProductQuestion q left join fetch q.options o " +
           "order by q.sortOrder asc, q.id asc, o.sortOrder asc, o.id asc")
    List<ProductQuestion> findAllWithOptionsOrder();
}