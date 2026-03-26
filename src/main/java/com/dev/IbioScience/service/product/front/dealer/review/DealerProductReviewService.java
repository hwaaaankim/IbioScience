package com.dev.IbioScience.service.product.front.dealer.review;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.front.productDetail.ProductDetailResponseDto.ReviewSummaryDto;
import com.dev.IbioScience.dto.front.productDetail.ReviewCreateResponse;
import com.dev.IbioScience.dto.front.productDetail.ReviewPermissionResponse;
import com.dev.IbioScience.dto.seller.product.review.DealerProductReviewImageViewDto;
import com.dev.IbioScience.dto.seller.product.review.DealerProductReviewViewDto;
import com.dev.IbioScience.enums.product.dealer.OrderItemProductType;
import com.dev.IbioScience.exception.ProductNotDisplayableException;
import com.dev.IbioScience.exception.ProductNotFoundException;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.product.dealer.DealerProduct;
import com.dev.IbioScience.model.product.dealer.review.DealerProductReview;
import com.dev.IbioScience.model.product.dealer.review.DealerProductReviewImage;
import com.dev.IbioScience.repository.auth.MemberRepository;
import com.dev.IbioScience.repository.product.dealer.DealerProductRepository;
import com.dev.IbioScience.repository.product.dealer.review.DealerProductReviewImageRepository;
import com.dev.IbioScience.repository.product.dealer.review.DealerProductReviewRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@RequiredArgsConstructor
@Slf4j
public class DealerProductReviewService {

    private static final int REVIEW_CREATE_LOCK_WAIT_SECONDS = 5;

    private final DealerProductRepository dealerProductRepository;
    private final DealerProductReviewRepository dealerProductReviewRepository;
    private final DealerProductReviewImageRepository dealerProductReviewImageRepository;
    private final MemberRepository memberRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${spring.upload.path}")
    private String uploadPath;

    @Transactional(readOnly = true)
    public ReviewPermissionResponse checkPermission(Long dealerProductId, Long memberId) {
        if (dealerProductId == null) {
            throw new IllegalArgumentException("딜러상품 ID가 필요합니다.");
        }

        DealerProduct dealerProduct = dealerProductRepository.findById(dealerProductId)
                .orElseThrow(() -> new ProductNotFoundException(dealerProductId));

        validateDisplayable(dealerProduct);

        if (memberId == null) {
            return ReviewPermissionResponse.builder()
                    .canWrite(false)
                    .message("리뷰 작성을 위해서는 로그인이 필요합니다.")
                    .build();
        }

        long purchasedOrderCount = countPaidDealerProductOrderCount(memberId, dealerProductId);
        if (purchasedOrderCount <= 0L) {
            return ReviewPermissionResponse.builder()
                    .canWrite(false)
                    .message("이 딜러상품을 구매한 회원만 리뷰를 작성할 수 있습니다.")
                    .build();
        }

        long writtenReviewCount = countDealerProductReviewCount(memberId, dealerProductId);
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

    @Transactional(readOnly = true)
    public ReviewSummaryDto getReviewSummary(Long dealerProductId) {
        List<DealerProductReview> reviews = dealerProductReviewRepository
                .findByDealerProductIdOrderByCreatedAtDesc(dealerProductId);

        if (reviews.isEmpty()) {
            return ReviewSummaryDto.builder()
                    .averageRating(0.0)
                    .reviewCount(0L)
                    .build();
        }

        double avg = reviews.stream()
                .filter(r -> r.getRating() != null)
                .mapToInt(DealerProductReview::getRating)
                .average()
                .orElse(0.0);

        return ReviewSummaryDto.builder()
                .averageRating(avg)
                .reviewCount((long) reviews.size())
                .build();
    }

    @Transactional(readOnly = true)
    public List<DealerProductReviewViewDto> getReviews(Long dealerProductId) {
        List<DealerProductReview> reviews = dealerProductReviewRepository
                .findByDealerProductIdOrderByCreatedAtDesc(dealerProductId);

        return reviews.stream()
                .map(this::toViewDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewCreateResponse createReview(Long dealerProductId,
                                             Long memberId,
                                             Integer rating,
                                             String content,
                                             List<MultipartFile> images) throws IOException {

        validateRatingAndContent(rating, content);

        DealerProduct dealerProduct = dealerProductRepository.findById(dealerProductId)
                .orElseThrow(() -> new IllegalArgumentException("딜러상품을 찾을 수 없습니다."));

        validateDisplayable(dealerProduct);

        String lockName = buildCreateLockName(memberId, dealerProductId);
        boolean locked = acquireNamedLock(lockName);

        if (!locked) {
            throw new IllegalStateException("동일 상품 리뷰 처리 중입니다. 잠시 후 다시 시도해 주세요.");
        }

        try {
            ReviewPermissionResponse perm = checkPermission(dealerProductId, memberId);
            if (!perm.isCanWrite()) {
                throw new IllegalStateException(perm.getMessage());
            }

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

            DealerProductReview review = new DealerProductReview();
            review.setDealerProduct(dealerProduct);
            review.setMemberId(memberId);
            review.setMemberDisplayName(resolveMemberDisplayName(member));
            review.setRating(rating);
            review.setContent(content.trim());

            dealerProductReviewRepository.saveAndFlush(review);

            if (images != null && !images.isEmpty()) {
                saveReviewImages(review, images, 0);
            }

            return ReviewCreateResponse.builder()
                    .reviewId(review.getId())
                    .build();
        } finally {
            releaseNamedLock(lockName);
        }
    }

    @Transactional
    public ReviewCreateResponse updateReview(Long dealerProductId,
                                             Long reviewId,
                                             Long memberId,
                                             Integer rating,
                                             String content,
                                             List<Long> deleteImageIds,
                                             List<MultipartFile> newImages) throws IOException {

        validateRatingAndContent(rating, content);

        DealerProductReview review = getOwnedReview(dealerProductId, reviewId, memberId);
        validateDisplayable(review.getDealerProduct());

        review.setRating(rating);
        review.setContent(content.trim());

        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            deleteSelectedReviewImages(review, deleteImageIds);
        }

        int nextSortOrder = getNextSortOrder(review.getId());

        if (newImages != null && !newImages.isEmpty()) {
            saveReviewImages(review, newImages, nextSortOrder);
        }

        return ReviewCreateResponse.builder()
                .reviewId(review.getId())
                .build();
    }

    @Transactional
    public void deleteReview(Long dealerProductId,
                             Long reviewId,
                             Long memberId) throws IOException {

        DealerProductReview review = getOwnedReview(dealerProductId, reviewId, memberId);

        deleteAllReviewImages(review.getId());
        dealerProductReviewRepository.delete(review);
    }

    private DealerProductReviewViewDto toViewDto(DealerProductReview review) {
        List<DealerProductReviewImageViewDto> images = dealerProductReviewImageRepository
                .findByReviewIdOrderBySortOrderAscIdAsc(review.getId())
                .stream()
                .map(img -> DealerProductReviewImageViewDto.builder()
                        .id(img.getId())
                        .url(img.getUrl())
                        .originalFilename(img.getOriginalFilename())
                        .build())
                .collect(Collectors.toList());

        return DealerProductReviewViewDto.builder()
                .id(review.getId())
                .memberId(review.getMemberId())
                .memberDisplayName(review.getMemberDisplayName())
                .rating(review.getRating())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .images(images)
                .build();
    }

    private DealerProductReview getOwnedReview(Long dealerProductId, Long reviewId, Long memberId) {
        DealerProductReview review = dealerProductReviewRepository
                .findByIdAndDealerProductId(reviewId, dealerProductId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        if (!Objects.equals(review.getMemberId(), memberId)) {
            throw new IllegalStateException("본인이 작성한 리뷰만 처리할 수 있습니다.");
        }

        return review;
    }

    private void validateDisplayable(DealerProduct dealerProduct) {
        if (dealerProduct.getDisplayStatus() != null
                && "OFF".equals(dealerProduct.getDisplayStatus().name())) {
            throw new ProductNotDisplayableException(dealerProduct.getId(), "진열하지 않는 딜러상품입니다.");
        }

        if (dealerProduct.getState() != null
                && !"NORMAL".equals(dealerProduct.getState().name())) {
            throw new ProductNotDisplayableException(dealerProduct.getId(), "삭제되었거나 삭제대기중인 딜러상품입니다.");
        }

        if (dealerProduct.getSaleStatus() != null
                && "OFF".equals(dealerProduct.getSaleStatus().name())) {
            throw new ProductNotDisplayableException(dealerProduct.getId(), "판매하지 않는 딜러상품입니다.");
        }
    }

    private void validateRatingAndContent(Integer rating, String content) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("별점은 1~5 사이의 값이어야 합니다.");
        }

        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("리뷰 내용을 입력해 주세요.");
        }
    }

    private String resolveMemberDisplayName(Member member) {
        if (member == null) {
            return null;
        }

        if (StringUtils.hasText(member.getName())) {
            return member.getName();
        }

        if (StringUtils.hasText(member.getUsername())) {
            return member.getUsername();
        }

        return "회원";
    }

    private long countPaidDealerProductOrderCount(Long memberId, Long dealerProductId) {
        Long count = entityManager.createQuery(
                        "select count(distinct oi.order.id) " +
                        "from OrderItem oi " +
                        "where oi.order.member.id = :memberId " +
                        "  and oi.order.paidAt is not null " +
                        "  and oi.itemProductType = :itemProductType " +
                        "  and oi.dealerProduct.id = :dealerProductId", Long.class)
                .setParameter("memberId", memberId)
                .setParameter("itemProductType", OrderItemProductType.DEALER)
                .setParameter("dealerProductId", dealerProductId)
                .getSingleResult();

        return count != null ? count : 0L;
    }

    private long countDealerProductReviewCount(Long memberId, Long dealerProductId) {
        Long count = entityManager.createQuery(
                        "select count(dr.id) " +
                        "from DealerProductReview dr " +
                        "where dr.memberId = :memberId " +
                        "  and dr.dealerProduct.id = :dealerProductId", Long.class)
                .setParameter("memberId", memberId)
                .setParameter("dealerProductId", dealerProductId)
                .getSingleResult();

        return count != null ? count : 0L;
    }

    private void deleteSelectedReviewImages(DealerProductReview review, Collection<Long> deleteImageIds) throws IOException {
        List<DealerProductReviewImage> targets = dealerProductReviewImageRepository
                .findByIdInAndReviewId(deleteImageIds, review.getId());

        for (DealerProductReviewImage image : targets) {
            deletePhysicalFile(image.getPath());
        }

        if (!targets.isEmpty()) {
            dealerProductReviewImageRepository.deleteAllInBatch(targets);
        }
    }

    private void deleteAllReviewImages(Long reviewId) throws IOException {
        List<DealerProductReviewImage> images = dealerProductReviewImageRepository
                .findByReviewIdOrderBySortOrderAscIdAsc(reviewId);

        for (DealerProductReviewImage image : images) {
            deletePhysicalFile(image.getPath());
        }

        if (!images.isEmpty()) {
            dealerProductReviewImageRepository.deleteAllInBatch(images);
        }
    }

    private void deletePhysicalFile(String filePath) throws IOException {
        if (!StringUtils.hasText(filePath)) {
            return;
        }

        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (Exception e) {
            log.warn("딜러리뷰 이미지 파일 삭제 실패. path={}", filePath, e);
        }
    }

    private int getNextSortOrder(Long reviewId) {
        List<DealerProductReviewImage> images = dealerProductReviewImageRepository
                .findByReviewIdOrderBySortOrderAscIdAsc(reviewId);

        if (images.isEmpty()) {
            return 0;
        }

        Integer maxSortOrder = images.stream()
                .map(DealerProductReviewImage::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(-1);

        return maxSortOrder + 1;
    }

    private void saveReviewImages(DealerProductReview review,
                                  List<MultipartFile> images,
                                  int startSortOrder) throws IOException {

        if (images == null) {
            return;
        }

        int sort = startSortOrder;
        Long dealerProductId = review.getDealerProduct().getId();
        Long memberId = review.getMemberId();

        String dateFolder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Path baseDir = Paths.get(
                uploadPath,
                "dealerProduct",
                String.valueOf(dealerProductId),
                "review",
                String.valueOf(memberId),
                dateFolder
        );

        if (!Files.exists(baseDir)) {
            Files.createDirectories(baseDir);
        }

        for (MultipartFile file : images) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
                throw new IllegalArgumentException("리뷰 이미지에는 이미지 파일만 업로드할 수 있습니다.");
            }

            String ext = getExtension(file.getOriginalFilename());
            String savedFileName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);

            Path target = baseDir.resolve(savedFileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String url = "/upload/dealerProduct/" + dealerProductId + "/review/" + memberId + "/" + dateFolder + "/" + savedFileName;

            DealerProductReviewImage image = new DealerProductReviewImage();
            image.setReview(review);
            image.setPath(target.toString());
            image.setUrl(url);
            image.setFileName(savedFileName);
            image.setOriginalFilename(file.getOriginalFilename());
            image.setSize((int) file.getSize());
            image.setSortOrder(sort++);
            image.setUploadedAt(LocalDateTime.now());

            dealerProductReviewImageRepository.save(image);
        }
    }

    private String getExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "";
        }

        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return "";
        }

        return originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String buildCreateLockName(Long memberId, Long dealerProductId) {
        return "dealer_review_create_" + memberId + "_" + dealerProductId;
    }

    private boolean acquireNamedLock(String lockName) {
        Object result = entityManager.createNativeQuery("SELECT GET_LOCK(:lockName, :waitSeconds)")
                .setParameter("lockName", lockName)
                .setParameter("waitSeconds", REVIEW_CREATE_LOCK_WAIT_SECONDS)
                .getSingleResult();

        if (!(result instanceof Number number)) {
            return false;
        }

        return number.intValue() == 1;
    }

    private void releaseNamedLock(String lockName) {
        try {
            entityManager.createNativeQuery("SELECT RELEASE_LOCK(:lockName)")
                    .setParameter("lockName", lockName)
                    .getSingleResult();
        } catch (Exception e) {
            log.warn("리뷰 named lock 해제 실패. lockName={}", lockName, e);
        }
    }
}