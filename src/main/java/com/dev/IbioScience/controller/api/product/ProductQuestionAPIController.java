package com.dev.IbioScience.controller.api.product;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.ProductQuestionApiDTO;
import com.dev.IbioScience.dto.ProductQuestionDTO;
import com.dev.IbioScience.model.product.ProductQuestion;
import com.dev.IbioScience.service.product.ProductQuestionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/display-questions")
@RequiredArgsConstructor
public class ProductQuestionAPIController {

    private final ProductQuestionService productQuestionService;

    @GetMapping
    public ResponseEntity<List<ProductQuestion>> getAllQuestions() {
        return ResponseEntity.ok(productQuestionService.findAllQuestions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductQuestion> getQuestion(@PathVariable Long id) {
        return ResponseEntity.ok(productQuestionService.findQuestion(id));
    }

    @PreAuthorize("@adminMenuFacade.canCreateOrUpdateByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).PROD_DISPLAY_MANAGER)")
    @PostMapping
    public ResponseEntity<?> saveQuestions(@RequestBody List<ProductQuestionDTO> questionDtos) {
        try {
            productQuestionService.saveAllQuestions(questionDtos);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("@adminMenuFacade.canDeleteByPageCode(T(com.dev.IbioScience.enums.auth.role.AdminPageCodes).PROD_DISPLAY_MANAGER)")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long id) {
        try {
            productQuestionService.deleteQuestion(id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/list-common")
    public List<ProductQuestionApiDTO> getCommonQuestionList() {
        return productQuestionService.getAllQuestions();
    }
}