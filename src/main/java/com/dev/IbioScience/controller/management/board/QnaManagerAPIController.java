package com.dev.IbioScience.controller.management.board;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.model.board.qna.QnaCategory;
import com.dev.IbioScience.service.board.qna.QnaCategoryService;
import com.dev.IbioScience.service.board.qna.QnaFileStorageService;
import com.dev.IbioScience.service.board.qna.QnaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
public class QnaManagerAPIController {

    private final QnaService qnaService;
    private final QnaCategoryService categoryService;
    private final QnaFileStorageService storageService;

    // =========================
    // CKEditor temp upload
    // =========================
    @PostMapping("/qna/upload-temp")
    public ResponseEntity<?> uploadTemp(@RequestParam("upload") MultipartFile upload) throws IOException {
        String url = storageService.saveTemp(upload);
        return ResponseEntity.ok(Map.of("url", url));
    }

    // =========================
    // QNA CRUD
    // =========================
    @PostMapping("/qna")
    public ResponseEntity<?> createQna(@RequestBody QnaCreateReq req) throws IOException {
        Long id = qnaService.create(req.categoryId(), req.title(), req.contentHtml(), req.writerMemberId());
        return ResponseEntity.ok(Map.of("id", id));
    }

    @PutMapping("/qna/{id}")
    public ResponseEntity<?> updateQna(@PathVariable Long id, @RequestBody QnaUpdateReq req) throws IOException {
        qnaService.update(id, req.categoryId(), req.title(), req.contentHtml());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/qna/{id}")
    public ResponseEntity<?> deleteQna(@PathVariable Long id) {
        qnaService.delete(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // =========================
    // Category CRUD
    // =========================
    @PostMapping("/qna-category")
    public ResponseEntity<?> createCategory(@RequestBody CategoryCreateReq req) {
        QnaCategory c = categoryService.create(req.name());
        return ResponseEntity.ok(Map.of("id", c.getId(), "name", c.getName()));
    }

    @PutMapping("/qna-category/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody CategoryUpdateReq req) {
        QnaCategory c = categoryService.update(id, req.name());
        return ResponseEntity.ok(Map.of("id", c.getId(), "name", c.getName()));
    }

    @DeleteMapping("/qna-category/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // =========================
    // DTO
    // =========================
    public record QnaCreateReq(Long categoryId, String title, String contentHtml, Long writerMemberId) {}
    public record QnaUpdateReq(Long categoryId, String title, String contentHtml) {}
    public record CategoryCreateReq(String name) {}
    public record CategoryUpdateReq(String name) {}
}
