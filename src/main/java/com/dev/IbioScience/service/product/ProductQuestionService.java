package com.dev.IbioScience.service.product;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.ProductQuestionApiDTO;
import com.dev.IbioScience.dto.ProductQuestionDTO;
import com.dev.IbioScience.enums.product.QuestionType;
import com.dev.IbioScience.model.product.ProductQuestion;
import com.dev.IbioScience.model.product.ProductQuestionOption;
import com.dev.IbioScience.repository.product.ProductQuestionOptionRepository;
import com.dev.IbioScience.repository.product.ProductQuestionRepository;
import com.dev.IbioScience.repository.product.register.ProductAnswerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductQuestionService {

    private final ProductQuestionRepository productQuestionRepository;
    private final ProductQuestionOptionRepository optionRepository;
    private final ProductAnswerRepository productAnswerRepository; // 추가
    
    public List<ProductQuestionApiDTO> getAllQuestions() {

        // ✅ required=true(=노출 + 필수)만 조회
        List<ProductQuestion> questions = productQuestionRepository.findByRequiredTrueOrderBySortOrderAsc();

        return questions.stream()
            .map(q -> {
                List<ProductQuestionOption> options =
                    optionRepository.findByQuestionIdOrderBySortOrderAsc(q.getId());
                return ProductQuestionApiDTO.from(q, options);
            })
            .collect(Collectors.toList());
    }
    
    // 1. 전체조회 (옵션 포함)
    @Transactional(readOnly = true)
    public List<ProductQuestion> findAllQuestions() {
        return productQuestionRepository.findAllByOrderBySortOrderAsc();
    }

    // 2. 단건조회
    @Transactional(readOnly = true)
    public ProductQuestion findQuestion(Long id) {
        return productQuestionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("해당 질문이 존재하지 않습니다."));
    }

    // 3. 전체 저장/수정
    @Transactional
    public void saveAllQuestions(List<ProductQuestionDTO> dtos) {
        // 기존 전체
        List<ProductQuestion> existing = productQuestionRepository.findAll();
        Map<Long, ProductQuestion> existingMap = existing.stream()
                .collect(Collectors.toMap(ProductQuestion::getId, q -> q));

        // 프론트에서 온 ID 집합
        Set<Long> incomingIds = dtos.stream()
                .filter(dto -> dto.getId() != null)
                .map(ProductQuestionDTO::getId)
                .collect(Collectors.toSet());

        // === [중요] 삭제 대상 선별 & 삭제 차단 로직 ===
        List<ProductQuestion> toDelete = existing.stream()
                .filter(q -> !incomingIds.contains(q.getId()))
                .collect(Collectors.toList());

        for (ProductQuestion q : toDelete) {
            if (productAnswerRepository.existsByQuestionId(q.getId())) {
                long cnt = productAnswerRepository.countByQuestionId(q.getId());
                throw new IllegalStateException(
                    "등록된 제품이 있어 삭제가 불가능합니다. 사용하지않음 을 이용해 주세요 (질문ID: " + q.getId() + ", 참조 답변 수: " + cnt + ")"
                );
            }
        }
        // 실제 삭제
        toDelete.forEach(productQuestionRepository::delete);

        // === 저장/수정 ===
        for (int i = 0; i < dtos.size(); i++) {
            ProductQuestionDTO dto = dtos.get(i);

            ProductQuestion question = (dto.getId() != null && existingMap.containsKey(dto.getId()))
                    ? existingMap.get(dto.getId())
                    : new ProductQuestion();

            question.setLabel(dto.getLabel());
            question.setPlaceholder(dto.getPlaceholder());
            question.setType(dto.getType());
            question.setRequired(dto.getRequired());   // UI의 표시함/숨김에 매핑 (숨김=required=false)
            question.setSortOrder(i);

            // 옵션 처리
            if (dto.getType() == QuestionType.SELECT && dto.getOptions() != null) {
                question.getOptions().clear();
                for (int j = 0; j < dto.getOptions().size(); j++) {
                    ProductQuestionOption option = new ProductQuestionOption();
                    option.setValue(dto.getOptions().get(j));
                    option.setSortOrder(j);
                    option.setQuestion(question);
                    question.getOptions().add(option);
                }
            } else {
                question.getOptions().clear();
            }

            productQuestionRepository.save(question);
        }
    }

    // 4. 단건삭제
    @Transactional
    public void deleteQuestion(Long id) {
        // 단건 삭제 API 사용시에도 동일한 제약
        if (productAnswerRepository.existsByQuestionId(id)) {
            long cnt = productAnswerRepository.countByQuestionId(id);
            throw new IllegalStateException(
                "등록된 제품이 있어 삭제가 불가능합니다. 사용하지않음 을 이용해 주세요 (질문ID: " + id + ", 참조 답변 수: " + cnt + ")"
            );
        }
        productQuestionRepository.deleteById(id);
    }
}