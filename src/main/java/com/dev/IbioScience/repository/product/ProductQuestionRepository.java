package com.dev.IbioScience.repository.product;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dev.IbioScience.model.product.ProductQuestion;

@Repository
public interface ProductQuestionRepository extends JpaRepository<ProductQuestion, Long> {
    List<ProductQuestion> findAllByOrderBySortOrderAsc();
}