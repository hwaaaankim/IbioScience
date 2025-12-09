package com.dev.IbioScience.service.product.front;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.front.productDetail.ReviewCreateResponse;
import com.dev.IbioScience.dto.front.productDetail.ReviewPermissionResponse;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.review.ProductReview;
import com.dev.IbioScience.model.product.review.ProductReviewImage;
import com.dev.IbioScience.repository.product.register.ProductRepository;
import com.dev.IbioScience.repository.product.review.ProductReviewImageRepository;
import com.dev.IbioScience.repository.product.review.ProductReviewRepository;
import com.dev.IbioScience.utils.FileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductReviewService {

    private final ProductRepository productRepository;
    private final ProductReviewRepository productReviewRepository;
    private final ProductReviewImageRepository productReviewImageRepository;
    private final FileStorageService fileStorageService;

    /**
     * 리뷰 작성 가능 여부 체크
     *
     * - 로그인 x → canWrite=false, "로그인이 필요합니다."
     * - 해당 상품 구매 이력 없음 → canWrite=false, "구매 후 작성 가능합니다."
     * - 이미 리뷰 있음 → canWrite=false, "이미 작성하신 리뷰가 있습니다."
     * - 나머지 → canWrite=true
     */
    @Transactional(readOnly = true)
    public ReviewPermissionResponse checkPermission(Long productId, Long memberId) {
        if (productId == null) {
            throw new IllegalArgumentException("상품 ID가 필요합니다.");
        }

        // 비로그인
        if (memberId == null) {
            return ReviewPermissionResponse.builder()
                    .canWrite(false)
                    .message("리뷰 작성을 위해서는 로그인이 필요합니다.")
                    .build();
        }

        // TODO: 실제 구매 이력 체크 로직 (주문 테이블 기준)
        boolean purchased = true; // 실제 구현에서 주문내역으로 검증

        if (!purchased) {
            return ReviewPermissionResponse.builder()
                    .canWrite(false)
                    .message("상품 구매 후에만 리뷰를 작성할 수 있습니다.")
                    .build();
        }

        boolean exists = productReviewRepository.existsByProductIdAndMemberId(productId, memberId);
        if (exists) {
            return ReviewPermissionResponse.builder()
                    .canWrite(false)
                    .message("이미 이 상품에 대한 리뷰를 작성하셨습니다.")
                    .build();
        }

        return ReviewPermissionResponse.builder()
                .canWrite(true)
                .message("리뷰를 작성할 수 있습니다.")
                .build();
    }

    /**
     * 리뷰 작성
     */
    @Transactional
    public ReviewCreateResponse createReview(Long productId,
                                             Long memberId,
                                             Integer rating,
                                             String content,
                                             List<MultipartFile> images) throws IOException {
        validateRatingAndContent(rating, content);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 권한 재검사 (중복 작성 방지)
        ReviewPermissionResponse perm = checkPermission(productId, memberId);
        if (!perm.isCanWrite()) {
            throw new IllegalStateException(perm.getMessage());
        }

        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setMemberId(memberId);
        review.setRating(rating);
        review.setContent(content);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());

        productReviewRepository.save(review);

        // 이미지 저장 (선택)
        if (images != null && !images.isEmpty()) {
            saveReviewImages(review, images);
        }

        return ReviewCreateResponse.builder()
                .reviewId(review.getId())
                .build();
    }

    /**
     * 리뷰 수정
     *
     * - 본인 리뷰인지 확인
     * - rating / content 필수
     * - 이미지 처리:
     *   * images == null or empty → 기존 이미지 전부 삭제
     *   * images 존재 → 기존 이미지 전부 삭제 후 새로 저장
     */
    @Transactional
    public ReviewCreateResponse updateReview(Long productId,
                                             Long reviewId,
                                             Long memberId,
                                             Integer rating,
                                             String content,
                                             List<MultipartFile> images) throws IOException {
        validateRatingAndContent(rating, content);

        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        if (!review.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("해당 상품의 리뷰가 아닙니다.");
        }

        if (!review.getMemberId().equals(memberId)) {
            throw new IllegalStateException("본인이 작성한 리뷰만 수정할 수 있습니다.");
        }

        review.setRating(rating);
        review.setContent(content);
        review.setUpdatedAt(LocalDateTime.now());

        // 기존 이미지 전부 삭제
        deleteAllReviewImages(review);

        // 새 이미지가 있으면 다시 저장
        if (images != null && !images.isEmpty()) {
            saveReviewImages(review, images);
        }

        return ReviewCreateResponse.builder()
                .reviewId(review.getId())
                .build();
    }

    /**
     * 리뷰 삭제
     *
     * - 본인 리뷰인지 확인
     * - 이미지(DB+파일) 먼저 삭제 후 리뷰 삭제
     */
    @Transactional
    public void deleteReview(Long productId,
                             Long reviewId,
                             Long memberId) throws IOException {
        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        if (!review.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("해당 상품의 리뷰가 아닙니다.");
        }

        if (!review.getMemberId().equals(memberId)) {
            throw new IllegalStateException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }

        // 이미지 삭제
        deleteAllReviewImages(review);

        // 리뷰 삭제
        productReviewRepository.delete(review);
    }

    private void validateRatingAndContent(Integer rating, String content) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("별점은 1~5 사이의 값이어야 합니다.");
        }
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("리뷰 내용을 입력해 주세요.");
        }
    }

    /**
     * 리뷰 이미지 전체 삭제 (DB + 파일)
     */
    private void deleteAllReviewImages(ProductReview review) throws IOException {
        List<ProductReviewImage> images = productReviewImageRepository.findByReviewId(review.getId());
        for (ProductReviewImage img : images) {
            if (StringUtils.hasText(img.getPath())) {
                try {
                    fileStorageService.delete(img.getPath());
                } catch (Exception e) {
                    log.warn("리뷰 이미지 파일 삭제 실패. path={}", img.getPath(), e);
                }
            }
        }
        productReviewImageRepository.deleteAll(images);
    }

    /**
     * 리뷰 이미지 저장 (파일 저장 + DB 저장)
     *
     * - 실제 파일은
     *   {spring.upload.path}/product/{productId}/review/{memberId}/{yyyyMMdd}/{uuid}.ext
     * - DB에는
     *   path : 실제 전체 경로
     *   url  : /upload/product/{productId}/review/{memberId}/{yyyyMMdd}/{uuid}.ext
     */
    private void saveReviewImages(ProductReview review, List<MultipartFile> images) throws IOException {
        int sort = 0;
        Long productId = review.getProduct().getId();
        Long memberId = review.getMemberId();
        Long reviewId = review.getId();

        for (MultipartFile file : images) {
            if (file.isEmpty()) continue;

            FileStorageService.FileSaveResult saveResult =
                    fileStorageService.saveReviewImage(productId, memberId, reviewId, file);

            ProductReviewImage img = new ProductReviewImage();
            img.setReview(review);
            img.setPath(saveResult.getPath());      // 실제 파일 경로 or S3 key
            img.setUrl(saveResult.getUrl());        // 접근 URL (/upload/...)
            img.setFileName(saveResult.getFileName());
            img.setOriginalFilename(file.getOriginalFilename());
            img.setSize((int) file.getSize());
            img.setSortOrder(sort++);
            img.setUploadedAt(LocalDateTime.now());

            productReviewImageRepository.save(img);
        }
    }
}

