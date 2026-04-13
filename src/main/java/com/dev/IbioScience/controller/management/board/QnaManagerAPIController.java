package com.dev.IbioScience.controller.management.board;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private static final String QNA_PAGE_CODE = "SITE_QNA_MANAGER";
    private static final String QNA_CATEGORY_PAGE_CODE = "SITE_QNA_CATEGORY_MANAGER";

    private final QnaService qnaService;
    private final QnaCategoryService categoryService;
    private final QnaFileStorageService storageService;

    // =========================
    // CKEditor temp upload
    // =========================
    @PreAuthorize("@adminMenuFacade.canCreateOrUpdateByPageCode('" + QNA_PAGE_CODE + "')")
    @PostMapping("/qna/upload-temp")
    public ResponseEntity<Map<String, Object>> uploadTemp(@RequestParam("upload") MultipartFile upload) throws IOException {
        String url = storageService.saveTemp(upload);
        return ResponseEntity.ok(Map.of("url", url));
    }

    // =========================
    // QNA CRUD
    // =========================
    @PreAuthorize("@adminMenuFacade.canCreateByPageCode('" + QNA_PAGE_CODE + "')")
    @PostMapping("/qna")
    public ResponseEntity<Map<String, Object>> createQna(@RequestBody QnaCreateReq req) throws IOException {
        Long id = qnaService.create(req.categoryId(), req.title(), req.contentHtml(), req.writerMemberId());
        return ResponseEntity.ok(Map.of("id", id));
    }

    @PreAuthorize("@adminMenuFacade.canUpdateByPageCode('" + QNA_PAGE_CODE + "')")
    @PutMapping("/qna/{id}")
    public ResponseEntity<Map<String, Object>> updateQna(@PathVariable Long id, @RequestBody QnaUpdateReq req) throws IOException {
        qnaService.update(id, req.categoryId(), req.title(), req.contentHtml());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PreAuthorize("@adminMenuFacade.canDeleteByPageCode('" + QNA_PAGE_CODE + "')")
    @DeleteMapping("/qna/{id}")
    public ResponseEntity<Map<String, Object>> deleteQna(@PathVariable Long id) {
        qnaService.delete(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // =========================
    // Category CRUD
    // =========================
    @PreAuthorize("@adminMenuFacade.canCreateByPageCode('" + QNA_CATEGORY_PAGE_CODE + "')")
    @PostMapping("/qna-category")
    public ResponseEntity<Map<String, Object>> createCategory(@RequestBody CategoryCreateReq req) {
        QnaCategory c = categoryService.create(req.name());
        return ResponseEntity.ok(Map.of("id", c.getId(), "name", c.getName()));
    }

    @PreAuthorize("@adminMenuFacade.canUpdateByPageCode('" + QNA_CATEGORY_PAGE_CODE + "')")
    @PutMapping("/qna-category/{id}")
    public ResponseEntity<Map<String, Object>> updateCategory(@PathVariable Long id, @RequestBody CategoryUpdateReq req) {
        QnaCategory c = categoryService.update(id, req.name());
        return ResponseEntity.ok(Map.of("id", c.getId(), "name", c.getName()));
    }

    @PreAuthorize("@adminMenuFacade.canDeleteByPageCode('" + QNA_CATEGORY_PAGE_CODE + "')")
    @DeleteMapping("/qna-category/{id}")
    public ResponseEntity<Map<String, Object>> deleteCategory(@PathVariable Long id) {
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