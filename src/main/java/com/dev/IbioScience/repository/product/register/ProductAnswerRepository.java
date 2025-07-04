package com.dev.IbioScience.repository.product.register;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.ProductAnswer;

public interface ProductAnswerRepository extends JpaRepository<ProductAnswer, Long> {
	// 답변ID 없이 product/question 기준 조회
	Optional<ProductAnswer> findByProductAndQuestionId(Product product, Long questionId);

}


