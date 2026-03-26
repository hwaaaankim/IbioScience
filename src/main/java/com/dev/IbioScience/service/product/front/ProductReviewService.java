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
import com.dev.IbioScience.enums.product.dealer.OrderItemProductType;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.review.ProductReview;
import com.dev.IbioScience.model.product.review.ProductReviewImage;
import com.dev.IbioScience.repository.product.register.ProductRepository;
import com.dev.IbioScience.repository.product.review.ProductReviewImageRepository;
import com.dev.IbioScience.repository.product.review.ProductReviewRepository;
import com.dev.IbioScience.utils.FileStorageService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 리뷰 작성 가능 여부 체크
     *
     * 기준:
     * - 로그인 x → 작성 불가
     * - 실제 결제 완료 주문 없음 → 작성 불가
     * - 결제 완료 주문 횟수 <= 이미 작성한 리뷰 수 → 작성 불가
     * - 결제 완료 주문 횟수 > 이미 작성한 리뷰 수 → 작성 가능
     *
     * 실제 구매 판정:
     * - tb_order.paid_at is not null
     * - 같은 상품을 여러 번 주문했더라도 "주문 1건 = 리뷰 1건"
     * - 같은 주문 안에서 수량이 2개여도 리뷰는 1건만 가능
     */
    @Transactional(readOnly = true)
    public ReviewPermissionResponse checkPermission(Long productId, Long memberId) {
        if (productId == null) {
            throw new IllegalArgumentException("상품 ID가 필요합니다.");
        }

        if (memberId == null) {
            return ReviewPermissionResponse.builder()
                    .canWrite(false)
                    .message("리뷰 작성을 위해서는 로그인이 필요합니다.")
                    .build();
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        long purchasedOrderCount = countPaidCompanyProductOrderCount(memberId, product.getId());
        if (purchasedOrderCount <= 0L) {
            return ReviewPermissionResponse.builder()
                    .canWrite(false)
                    .message("상품 구매 후에만 리뷰를 작성할 수 있습니다.")
                    .build();
        }

        long writtenReviewCount = countProductReviewCount(memberId, product.getId());
        if (writtenReviewCount >= purchasedOrderCount) {
            return ReviewPermissionResponse.builder()
                    .canWrite(false)
                    .message("구매하신 주문 건에 대한 리뷰를 모두 작성하셨습니다.")
                    .build();
        }

        long remaining = purchasedOrderCount - writtenReviewCount;

        return ReviewPermissionResponse.builder()
                .canWrite(true)
                .message("리뷰를 작성할 수 있습니다. 남은 작성 가능 횟수: " + remaining + "건")
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

        ReviewPermissionResponse perm = checkPermission(productId, memberId);
        if (!perm.isCanWrite()) {
            throw new IllegalStateException(perm.getMessage());
        }

        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setMemberId(memberId);
        review.setRating(rating);
        review.setContent(content.trim());
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());

        productReviewRepository.save(review);

        if (images != null && !images.isEmpty()) {
            saveReviewImages(review, images);
        }

        return ReviewCreateResponse.builder()
                .reviewId(review.getId())
                .build();
    }

    /**
     * 리뷰 수정
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
        review.setContent(content.trim());
        review.setUpdatedAt(LocalDateTime.now());

        deleteAllReviewImages(review);

        if (images != null && !images.isEmpty()) {
            saveReviewImages(review, images);
        }

        return ReviewCreateResponse.builder()
                .reviewId(review.getId())
                .build();
    }

    /**
     * 리뷰 삭제
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

        deleteAllReviewImages(review);
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
     * 회원이 실제 결제 완료한 "회사상품 주문 횟수"를 반환
     *
     * - distinct oi.order.id 로 계산
     * - 같은 주문에서 수량이 여러 개여도 1건으로 처리
     */
    private long countPaidCompanyProductOrderCount(Long memberId, Long productId) {
        Long count = entityManager.createQuery(
                        "select count(distinct oi.order.id) " +
                        "from OrderItem oi " +
                        "where oi.order.member.id = :memberId " +
                        "  and oi.order.paidAt is not null " +
                        "  and oi.itemProductType = :itemProductType " +
                        "  and oi.product.id = :productId", Long.class)
                .setParameter("memberId", memberId)
                .setParameter("itemProductType", OrderItemProductType.COMPANY)
                .setParameter("productId", productId)
                .getSingleResult();

        return count != null ? count : 0L;
    }

    /**
     * 회원이 해당 회사상품에 대해 작성한 리뷰 수
     */
    private long countProductReviewCount(Long memberId, Long productId) {
        Long count = entityManager.createQuery(
                        "select count(pr.id) " +
                        "from ProductReview pr " +
                        "where pr.memberId = :memberId " +
                        "  and pr.product.id = :productId", Long.class)
                .setParameter("memberId", memberId)
                .setParameter("productId", productId)
                .getSingleResult();

        return count != null ? count : 0L;
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
     */
    private void saveReviewImages(ProductReview review, List<MultipartFile> images) throws IOException {
        int sort = 0;
        Long productId = review.getProduct().getId();
        Long memberId = review.getMemberId();
        Long reviewId = review.getId();

        for (MultipartFile file : images) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            FileStorageService.FileSaveResult saveResult =
                    fileStorageService.saveReviewImage(productId, memberId, reviewId, file);

            ProductReviewImage img = new ProductReviewImage();
            img.setReview(review);
            img.setPath(saveResult.getPath());
            img.setUrl(saveResult.getUrl());
            img.setFileName(saveResult.getFileName());
            img.setOriginalFilename(file.getOriginalFilename());
            img.setSize((int) file.getSize());
            img.setSortOrder(sort++);
            img.setUploadedAt(LocalDateTime.now());

            productReviewImageRepository.save(img);
        }
    }
}
