package com.dev.IbioScience.service.product;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.ProductQuestionApiDto;
import com.dev.IbioScience.dto.ProductQuestionDto;
import com.dev.IbioScience.model.product.ProductQuestion;
import com.dev.IbioScience.model.product.ProductQuestionOption;
import com.dev.IbioScience.model.product.status.QuestionType;
import com.dev.IbioScience.repository.product.ProductQuestionOptionRepository;
import com.dev.IbioScience.repository.product.ProductQuestionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductQuestionService {

    private final ProductQuestionRepository productQuestionRepository;
    private final ProductQuestionOptionRepository optionRepository;
    
    public List<ProductQuestionApiDto> getAllQuestions() {
        List<ProductQuestion> questions = productQuestionRepository.findAllByOrderBySortOrderAsc();
        return questions.stream()
            .map(q -> {
                List<ProductQuestionOption> options = optionRepository.findByQuestionIdOrderBySortOrderAsc(q.getId());
                return ProductQuestionApiDto.from(q, options);
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
    public void saveAllQuestions(List<ProductQuestionDto> dtos) {
        // 기존 전체 목록 조회
        List<ProductQuestion> existing = productQuestionRepository.findAll();
        Map<Long, ProductQuestion> existingMap = existing.stream()
                .collect(Collectors.toMap(ProductQuestion::getId, q -> q));

        Set<Long> incomingIds = dtos.stream()
                .filter(dto -> dto.getId() != null)
                .map(ProductQuestionDto::getId)
                .collect(Collectors.toSet());

        // 3-1. 삭제 처리 (DB에 있으나, 프론트에는 없는 ID)
        existing.stream()
                .filter(q -> !incomingIds.contains(q.getId()))
                .forEach(productQuestionRepository::delete);

        // 3-2. 저장/수정 처리
        for (int i = 0; i < dtos.size(); i++) {
            ProductQuestionDto dto = dtos.get(i);

            ProductQuestion question = (dto.getId() != null && existingMap.containsKey(dto.getId()))
                    ? existingMap.get(dto.getId())
                    : new ProductQuestion();

            question.setLabel(dto.getLabel());
            question.setPlaceholder(dto.getPlaceholder());
            question.setType(dto.getType());
            question.setRequired(dto.getRequired());
            question.setSortOrder(i);
            // display 필드가 실제 DB에 필요하다면 추가

            // 옵션 처리 (SELECT 타입만)
            if (dto.getType() == QuestionType.SELECT && dto.getOptions() != null) {
                // 기존 옵션 모두 삭제 후, 새로 생성(더 세밀하게 할 수도 있음)
                question.getOptions().clear();
                for (int j = 0; j < dto.getOptions().size(); j++) {
                    ProductQuestionOption option = new ProductQuestionOption();
                    option.setValue(dto.getOptions().get(j));
                    option.setSortOrder(j);
                    option.setQuestion(question);
                    question.getOptions().add(option);
                }
            } else {
                // 다른 타입일 때 옵션 모두 제거
                question.getOptions().clear();
            }

            productQuestionRepository.save(question);
        }
    }

    // 4. 단건삭제
    @Transactional
    public void deleteQuestion(Long id) {
        productQuestionRepository.deleteById(id);
    }
}