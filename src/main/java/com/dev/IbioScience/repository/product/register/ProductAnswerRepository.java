package com.dev.IbioScience.repository.product.register;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.ProductAnswer;
import com.dev.IbioScience.model.product.ProductQuestion;

public interface ProductAnswerRepository extends JpaRepository<ProductAnswer, Long> {
	// 답변ID 없이 product/question 기준 조회
	Optional<ProductAnswer> findByProductAndQuestionId(Product product, Long questionId);
	Optional<ProductAnswer> findByProductIdAndQuestionId(Long productId, Long questionId);
	boolean existsByQuestionId(Long questionId);
    long countByQuestionId(Long questionId);
    
    Optional<ProductAnswer> findTopByProductIdAndQuestionIdOrderByIdAsc(Long productId, Long questionId);

    @Query("select a from ProductAnswer a " +
           "join fetch a.question q " +
           "where a.product.id = :pid " +
           "order by q.sortOrder asc, q.id asc, a.id asc")
    List<ProductAnswer> findByProductWithQuestion(@Param("pid") Long productId);
    
    /**
     * 제품별 공통질문/답변 조회 (required=true 질문만)
     *
     * - ProductAnswer.question            : 즉시 로딩
     * - ProductAnswer.detailImages        : 즉시 로딩 (bag 1개)
     * - ProductQuestion.options           : LAZY 로딩 (필요 시 트랜잭션 내에서 사용)
     *
     * ⇒ MultipleBagFetchException 회피:
     *    detailImages, options 두 개를 동시에 fetch 하지 않고,
     *    detailImages만 fetch, options는 나중에 Lazy 로딩.
     */
    @EntityGraph(attributePaths = {
        "question",
        "detailImages"
    })
    @Query("""
        select pa
        from ProductAnswer pa
        where pa.product.id = :productId
          and pa.question.required = true
        order by pa.question.sortOrder asc, pa.id asc
        """)
    List<ProductAnswer> findAllWithQuestionAndImagesByProductId(@Param("productId") Long productId);
    
    Optional<ProductAnswer> findFirstByProductAndQuestionOrderByIdAsc(Product product, ProductQuestion question);
    
}


