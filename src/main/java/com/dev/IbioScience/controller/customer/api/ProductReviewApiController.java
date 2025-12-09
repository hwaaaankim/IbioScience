package com.dev.IbioScience.controller.customer.api;

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
import com.dev.IbioScience.service.product.front.ProductReviewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/front/product")
@RequiredArgsConstructor
@Slf4j
public class ProductReviewApiController {

    private final ProductReviewService productReviewService;

    /**
     * 리뷰 작성 가능 여부 체크
     *
     * - 로그인 상태면 memberId 기반으로 "구매 여부 + 중복 리뷰 여부" 등을 체크
     * - 비로그인 상태라면 memberId = null 로 서비스에 넘기고
     *   서비스 내부에서 "로그인 필요" 상태를 응답하도록 설계
     */
    @GetMapping("/{productId}/review/permission")
    public ResponseEntity<ReviewPermissionResponse> getReviewPermission(@PathVariable Long productId,
                                                                        @AuthenticationPrincipal PrincipalDetails principal) {

        Long memberId = null;
        if (principal != null && principal.getMember() != null) {
            memberId = principal.getMember().getId();
        }

        ReviewPermissionResponse response = productReviewService.checkPermission(productId, memberId);
        return ResponseEntity.ok(response);
    }

    /**
     * 리뷰 작성
     *
     * multipart/form-data
     * - rating: 1~5 (필수)
     * - content: 텍스트 (필수)
     * - images: 다중 파일 (선택)
     */
    @PostMapping("/{productId}/review")
    public ResponseEntity<?> createReview(@PathVariable Long productId,
                                          @RequestParam("rating") Integer rating,
                                          @RequestParam("content") String content,
                                          @RequestParam(value = "images", required = false) List<MultipartFile> images,
                                          @AuthenticationPrincipal PrincipalDetails principal) {
        try {
            // 0) 로그인 여부 검사
            if (principal == null || principal.getMember() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("리뷰 작성을 위해서는 로그인이 필요합니다.");
            }

            Long memberId = principal.getMember().getId();

            // 1) 서비스 호출
            ReviewCreateResponse result = productReviewService.createReview(
                    productId, memberId, rating, content, images
            );

            // 2) 성공 응답
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.warn("리뷰 작성 파라미터 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IllegalStateException e) {
            log.warn("리뷰 작성 상태 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());

        } catch (IOException e) {
            log.error("리뷰 이미지 저장 중 오류", e);
            return ResponseEntity.internalServerError().body("리뷰 이미지 저장 중 오류가 발생했습니다.");

        } catch (Exception e) {
            log.error("리뷰 작성 중 알 수 없는 오류", e);
            return ResponseEntity.internalServerError().body("리뷰 작성 중 오류가 발생했습니다.");
        }
    }

    /**
     * 리뷰 수정
     *
     * - 본인 리뷰만 수정 가능
     * - rating / content 필수
     * - images:
     *   * null 또는 비어있음 → 기존 이미지 모두 삭제
     *   * 1개 이상 전달 → 기존 이미지 모두 삭제 후 새 이미지로 교체
     */
    @PutMapping("/{productId}/review/{reviewId}")
    public ResponseEntity<?> updateReview(@PathVariable Long productId,
                                          @PathVariable Long reviewId,
                                          @RequestParam("rating") Integer rating,
                                          @RequestParam("content") String content,
                                          @RequestParam(value = "images", required = false) List<MultipartFile> images,
                                          @AuthenticationPrincipal PrincipalDetails principal) {
        try {
            if (principal == null || principal.getMember() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("리뷰 수정을 위해서는 로그인이 필요합니다.");
            }

            Long memberId = principal.getMember().getId();

            ReviewCreateResponse result = productReviewService.updateReview(
                    productId, reviewId, memberId, rating, content, images
            );

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.warn("리뷰 수정 파라미터 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IllegalStateException e) {
            // 예: 본인 리뷰가 아님, 구매 이력 없음 등
            log.warn("리뷰 수정 상태 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());

        } catch (IOException e) {
            log.error("리뷰 이미지 수정 중 오류", e);
            return ResponseEntity.internalServerError().body("리뷰 이미지 수정 중 오류가 발생했습니다.");

        } catch (Exception e) {
            log.error("리뷰 수정 중 알 수 없는 오류", e);
            return ResponseEntity.internalServerError().body("리뷰 수정 중 오류가 발생했습니다.");
        }
    }

    /**
     * 리뷰 삭제
     *
     * - 본인 리뷰만 삭제 가능
     * - DB 및 이미지 파일 모두 삭제
     */
    @DeleteMapping("/{productId}/review/{reviewId}")
    public ResponseEntity<?> deleteReview(@PathVariable Long productId,
                                          @PathVariable Long reviewId,
                                          @AuthenticationPrincipal PrincipalDetails principal) {
        try {
            if (principal == null || principal.getMember() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("리뷰 삭제를 위해서는 로그인이 필요합니다.");
            }

            Long memberId = principal.getMember().getId();
            productReviewService.deleteReview(productId, reviewId, memberId);

            return ResponseEntity.ok("OK");

        } catch (IllegalArgumentException e) {
            log.warn("리뷰 삭제 파라미터 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IllegalStateException e) {
            log.warn("리뷰 삭제 상태 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());

        } catch (IOException e) {
            log.error("리뷰 이미지 삭제 중 오류", e);
            return ResponseEntity.internalServerError().body("리뷰 이미지 삭제 중 오류가 발생했습니다.");

        } catch (Exception e) {
            log.error("리뷰 삭제 중 알 수 없는 오류", e);
            return ResponseEntity.internalServerError().body("리뷰 삭제 중 오류가 발생했습니다.");
        }
    }
}