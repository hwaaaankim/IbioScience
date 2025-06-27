package com.dev.IbioScience.controller.api.product;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.ProductQuestionApiDto;
import com.dev.IbioScience.dto.ProductQuestionDto;
import com.dev.IbioScience.model.product.ProductQuestion;
import com.dev.IbioScience.service.product.ProductQuestionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/display-questions")
@RequiredArgsConstructor
public class ProductQuestionAPIController {

    private final ProductQuestionService productQuestionService;

    // 1. 전체 리스트 조회
    @GetMapping
    public ResponseEntity<List<ProductQuestion>> getAllQuestions() {
        return ResponseEntity.ok(productQuestionService.findAllQuestions());
    }

    // 2. 단건 상세조회
    @GetMapping("/{id}")
    public ResponseEntity<ProductQuestion> getQuestion(@PathVariable Long id) {
        return ResponseEntity.ok(productQuestionService.findQuestion(id));
    }

    // 3. 전체 저장/수정(리스트 통째로)
    @PostMapping
    public ResponseEntity<Void> saveQuestions(@RequestBody List<ProductQuestionDto> questionDtos) {
        productQuestionService.saveAllQuestions(questionDtos);
        return ResponseEntity.ok().build();
    }

    // 4. 단건 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        productQuestionService.deleteQuestion(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/list-common")
    public List<ProductQuestionApiDto> getCommonQuestionList() {
        return productQuestionService.getAllQuestions();
    }
}