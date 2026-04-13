package com.dev.IbioScience.controller.admin.api;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.admin.reviewList.AdminClientReviewDeleteRequest;
import com.dev.IbioScience.dto.admin.reviewList.AdminClientReviewDeleteResponse;
import com.dev.IbioScience.dto.admin.reviewList.AdminClientReviewPageResponse;
import com.dev.IbioScience.dto.admin.reviewList.AdminClientReviewSearchCondition;
import com.dev.IbioScience.service.auth.crm.review.AdminClientReviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/root/api/clientDetail/{memberId}/reviewList")
public class AdminClientReviewApiController {

    private static final String PAGE_CODE = "CRM_REVIEW_LIST";

    private final AdminClientReviewService adminClientReviewService;

    @PreAuthorize("@adminMenuFacade.canViewByPageCode('" + PAGE_CODE + "')")
    @GetMapping
    public AdminClientReviewPageResponse getReviewPage(@PathVariable Long memberId,
                                                       @ModelAttribute AdminClientReviewSearchCondition condition) {
        return adminClientReviewService.getReviewPage(memberId, condition);
    }

    @PreAuthorize("@adminMenuFacade.canDeleteByPageCode('" + PAGE_CODE + "')")
    @DeleteMapping
    public AdminClientReviewDeleteResponse deleteReviews(@PathVariable Long memberId,
                                                         @RequestBody AdminClientReviewDeleteRequest request) {
        return adminClientReviewService.deleteReviews(memberId, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", e.getMessage()));
    }
}