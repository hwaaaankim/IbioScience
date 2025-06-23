package com.dev.IbioScience.repository.product;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dev.IbioScience.model.product.ProductQuestionOption;

@Repository
public interface ProductQuestionOptionRepository extends JpaRepository<ProductQuestionOption, Long> { 
	List<ProductQuestionOption> findByQuestionIdOrderBySortOrderAsc(Long questionId);
}