package com.dev.IbioScience.service.auth.crm.review;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.dev.IbioScience.dto.admin.reviewList.AdminClientReviewDeleteRequest;
import com.dev.IbioScience.dto.admin.reviewList.AdminClientReviewDeleteResponse;
import com.dev.IbioScience.dto.admin.reviewList.AdminClientReviewListItem;
import com.dev.IbioScience.dto.admin.reviewList.AdminClientReviewPageResponse;
import com.dev.IbioScience.dto.admin.reviewList.AdminClientReviewSearchCondition;
import com.dev.IbioScience.model.product.review.ProductReview;
import com.dev.IbioScience.model.product.review.ProductReviewImage;
import com.dev.IbioScience.repository.auth.review.AdminClientReviewListRowProjection;
import com.dev.IbioScience.repository.auth.review.AdminClientReviewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminClientReviewService {

    private static final List<Integer> ALLOWED_PAGE_SIZES = List.of(10, 30, 50, 70, 100);
    private static final DateTimeFormatter CREATED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AdminClientReviewRepository adminClientReviewRepository;

    public AdminClientReviewPageResponse getReviewPage(Long memberId, AdminClientReviewSearchCondition condition) {
        validateSearchCondition(memberId, condition);

        int normalizedPage = Math.max(condition.getPage() == null ? 0 : condition.getPage(), 0);
        int normalizedSize = normalizePageSize(condition.getSize());

        LocalDateTime fromDateTime = condition.getFromDate() == null
                ? null
                : condition.getFromDate().atStartOfDay();

        LocalDateTime toDateTime = condition.getToDate() == null
                ? null
                : condition.getToDate().plusDays(1).atStartOfDay();

        PageRequest pageRequest = PageRequest.of(normalizedPage, normalizedSize);

        Page<AdminClientReviewListRowProjection> reviewPage =
                adminClientReviewRepository.searchReviewPage(memberId, fromDateTime, toDateTime, pageRequest);

        List<AdminClientReviewListItem> content = reviewPage.getContent().stream()
                .map(this::toListItem)
                .toList();

        return AdminClientReviewPageResponse.builder()
                .content(content)
                .page(normalizedPage)
                .size(normalizedSize)
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .numberOfElements(reviewPage.getNumberOfElements())
                .first(reviewPage.isFirst())
                .last(reviewPage.isLast())
                .empty(reviewPage.isEmpty())
                .build();
    }

    @Transactional
    public AdminClientReviewDeleteResponse deleteReviews(Long memberId, AdminClientReviewDeleteRequest request) {
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID가 없습니다.");
        }

        if (request == null || request.getReviewIds() == null || request.getReviewIds().isEmpty()) {
            throw new IllegalArgumentException("삭제할 리뷰를 선택해 주세요.");
        }

        Set<Long> distinctIds = new LinkedHashSet<>(request.getReviewIds());
        List<Long> reviewIds = new ArrayList<>(distinctIds);

        List<ProductReview> reviews =
                adminClientReviewRepository.findAllWithImagesByMemberIdAndIdIn(memberId, reviewIds);

        if (reviews.isEmpty()) {
            throw new IllegalArgumentException("삭제할 리뷰를 찾을 수 없습니다.");
        }

        for (ProductReview review : reviews) {
            if (review.getImages() == null || review.getImages().isEmpty()) {
                continue;
            }

            for (ProductReviewImage image : review.getImages()) {
                deletePhysicalFile(image.getPath());
            }
        }

        adminClientReviewRepository.deleteAll(reviews);
        adminClientReviewRepository.flush();

        return AdminClientReviewDeleteResponse.builder()
                .success(true)
                .deletedCount(reviews.size())
                .message(reviews.size() + "건의 리뷰가 삭제되었습니다.")
                .build();
    }

    private AdminClientReviewListItem toListItem(AdminClientReviewListRowProjection row) {
        return AdminClientReviewListItem.builder()
                .reviewId(row.getReviewId())
                .productId(row.getProductId())
                .authorName(row.getAuthorName())
                .thumbnailUrl(row.getFirstImageUrl())
                .imageCount(row.getImageCount() == null ? 0L : row.getImageCount())
                .rating(row.getRating())
                .createdAtText(row.getCreatedAt() == null ? "-" : row.getCreatedAt().format(CREATED_AT_FORMATTER))
                .content(row.getContent())
                .build();
    }

    private void validateSearchCondition(Long memberId, AdminClientReviewSearchCondition condition) {
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID가 없습니다.");
        }

        if (condition == null) {
            throw new IllegalArgumentException("검색 조건이 없습니다.");
        }

        if (condition.getFromDate() != null
                && condition.getToDate() != null
                && condition.getFromDate().isAfter(condition.getToDate())) {
            throw new IllegalArgumentException("조회 시작일은 종료일보다 늦을 수 없습니다.");
        }
    }

    private int normalizePageSize(Integer requestedSize) {
        if (requestedSize == null) {
            return 10;
        }

        return ALLOWED_PAGE_SIZES.contains(requestedSize) ? requestedSize : 10;
    }

    private void deletePhysicalFile(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return;
        }

        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException e) {
            log.error("리뷰 이미지 파일 삭제 실패. path={}", filePath, e);
            throw new IllegalStateException("리뷰 이미지 파일 삭제 중 오류가 발생했습니다.");
        }
    }
}