package com.dev.IbioScience.controller.seller.api;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.front.productDetail.ReviewCreateResponse;
import com.dev.IbioScience.dto.front.productDetail.ReviewPermissionResponse;
import com.dev.IbioScience.model.auth.PrincipalDetails;
import com.dev.IbioScience.service.product.front.dealer.review.DealerProductReviewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/front/dealer-product")
@RequiredArgsConstructor
@Slf4j
public class DealerProductReviewApiController {

    private final DealerProductReviewService dealerProductReviewService;

    @GetMapping("/{dealerProductId}/review/permission")
    public ResponseEntity<ReviewPermissionResponse> getReviewPermission(
            @PathVariable Long dealerProductId,
            @AuthenticationPrincipal PrincipalDetails principal) {

        Long memberId = null;
        if (principal != null && principal.getMember() != null) {
            memberId = principal.getMember().getId();
        }

        ReviewPermissionResponse response = dealerProductReviewService.checkPermission(dealerProductId, memberId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{dealerProductId}/review")
    public ResponseEntity<?> createReview(
            @PathVariable Long dealerProductId,
            @RequestParam("rating") Integer rating,
            @RequestParam("content") String content,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal PrincipalDetails principal) {

        try {
            if (principal == null || principal.getMember() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("리뷰 작성을 위해서는 로그인이 필요합니다.");
            }

            Long memberId = principal.getMember().getId();

            ReviewCreateResponse result = dealerProductReviewService.createReview(
                    dealerProductId, memberId, rating, content, images
            );

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.warn("딜러리뷰 작성 파라미터 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IllegalStateException e) {
            log.warn("딜러리뷰 작성 상태 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());

        } catch (IOException e) {
            log.error("딜러리뷰 이미지 저장 중 오류", e);
            return ResponseEntity.internalServerError().body("딜러리뷰 이미지 저장 중 오류가 발생했습니다.");

        } catch (Exception e) {
            log.error("딜러리뷰 작성 중 알 수 없는 오류", e);
            return ResponseEntity.internalServerError().body("딜러리뷰 작성 중 오류가 발생했습니다.");
        }
    }

    @PutMapping("/{dealerProductId}/review/{reviewId}")
    public ResponseEntity<?> updateReview(
            @PathVariable Long dealerProductId,
            @PathVariable Long reviewId,
            @RequestParam("rating") Integer rating,
            @RequestParam("content") String content,
            @RequestParam(value = "deleteImageIds", required = false) List<Long> deleteImageIds,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal PrincipalDetails principal) {

        try {
            if (principal == null || principal.getMember() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("리뷰 수정을 위해서는 로그인이 필요합니다.");
            }

            Long memberId = principal.getMember().getId();

            ReviewCreateResponse result = dealerProductReviewService.updateReview(
                    dealerProductId, reviewId, memberId, rating, content, deleteImageIds, images
            );

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.warn("딜러리뷰 수정 파라미터 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IllegalStateException e) {
            log.warn("딜러리뷰 수정 상태 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());

        } catch (IOException e) {
            log.error("딜러리뷰 이미지 수정 중 오류", e);
            return ResponseEntity.internalServerError().body("딜러리뷰 이미지 수정 중 오류가 발생했습니다.");

        } catch (Exception e) {
            log.error("딜러리뷰 수정 중 알 수 없는 오류", e);
            return ResponseEntity.internalServerError().body("딜러리뷰 수정 중 오류가 발생했습니다.");
        }
    }

    @DeleteMapping("/{dealerProductId}/review/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable Long dealerProductId,
            @PathVariable Long reviewId,
            @AuthenticationPrincipal PrincipalDetails principal) {

        try {
            if (principal == null || principal.getMember() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("리뷰 삭제를 위해서는 로그인이 필요합니다.");
            }

            Long memberId = principal.getMember().getId();
            dealerProductReviewService.deleteReview(dealerProductId, reviewId, memberId);

            return ResponseEntity.ok("OK");

        } catch (IllegalArgumentException e) {
            log.warn("딜러리뷰 삭제 파라미터 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IllegalStateException e) {
            log.warn("딜러리뷰 삭제 상태 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());

        } catch (IOException e) {
            log.error("딜러리뷰 이미지 삭제 중 오류", e);
            return ResponseEntity.internalServerError().body("딜러리뷰 이미지 삭제 중 오류가 발생했습니다.");

        } catch (Exception e) {
            log.error("딜러리뷰 삭제 중 알 수 없는 오류", e);
            return ResponseEntity.internalServerError().body("딜러리뷰 삭제 중 오류가 발생했습니다.");
        }
    }
}