package com.dev.IbioScience.service.product.front.dealer.review;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.seller.product.review.DealerProductReviewAdminRowDto;
import com.dev.IbioScience.dto.seller.product.review.DealerProductReviewAdminSearchCondition;
import com.dev.IbioScience.model.product.dealer.review.DealerProductReview;
import com.dev.IbioScience.model.product.dealer.review.DealerProductReviewImage;
import com.dev.IbioScience.repository.product.dealer.review.DealerProductReviewImageRepository;
import com.dev.IbioScience.repository.product.dealer.review.DealerProductReviewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DealerProductReviewAdminService {

    private final DealerProductReviewRepository dealerProductReviewRepository;
    private final DealerProductReviewImageRepository dealerProductReviewImageRepository;

    @Transactional(readOnly = true)
    public Page<DealerProductReviewAdminRowDto> getAdminReviewPage(DealerProductReviewAdminSearchCondition condition) {
        PageRequest pageable = PageRequest.of(condition.getPage(), condition.getSize());

        Page<DealerProductReviewAdminRowDto> page = dealerProductReviewRepository.searchAdminReviewPage(
                condition.getFromDate(),
                condition.getToDate(),
                pageable,
                condition.getSortField(),
                condition.getSortDir()
        );

        List<DealerProductReviewAdminRowDto> content = page.getContent();
        if (content.isEmpty()) {
            return page;
        }

        List<Long> reviewIds = content.stream()
                .map(DealerProductReviewAdminRowDto::getReviewId)
                .toList();

        List<DealerProductReviewImage> imageEntities =
                dealerProductReviewImageRepository.findByReview_IdInOrderBySortOrderAscIdAsc(reviewIds);

        Map<Long, List<DealerProductReviewImage>> imageMap = new LinkedHashMap<>();
        for (DealerProductReviewImage image : imageEntities) {
            Long reviewId = image.getReview().getId();
            imageMap.computeIfAbsent(reviewId, key -> new ArrayList<>()).add(image);
        }

        for (DealerProductReviewAdminRowDto row : content) {
            List<DealerProductReviewImage> images = imageMap.getOrDefault(row.getReviewId(), List.of());

            row.setImageCount(images.size());

            List<String> imageUrls = new ArrayList<>();
            for (DealerProductReviewImage image : images) {
                if (image.getUrl() != null && !image.getUrl().isBlank()) {
                    imageUrls.add(image.getUrl());
                }
            }

            row.setImageUrls(imageUrls);

            if (!imageUrls.isEmpty()) {
                row.setFirstImageUrl(imageUrls.get(0));
            }
        }

        return page;
    }

    @Transactional
    public int deleteSelectedReviews(List<Long> reviewIds) {
        if (reviewIds == null || reviewIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 리뷰를 선택해 주세요.");
        }

        List<DealerProductReview> reviews = dealerProductReviewRepository.findAllById(reviewIds);
        if (reviews.isEmpty()) {
            throw new IllegalArgumentException("삭제할 리뷰가 존재하지 않습니다.");
        }

        List<DealerProductReviewImage> images =
                dealerProductReviewImageRepository.findByReview_IdInOrderBySortOrderAscIdAsc(reviewIds);

        for (DealerProductReviewImage image : images) {
            deletePhysicalFile(image.getPath());
        }

        dealerProductReviewRepository.deleteAll(reviews);

        return reviews.size();
    }

    private void deletePhysicalFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }

        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
        } catch (Exception e) {
            log.warn("리뷰 이미지 파일 삭제 중 예외가 발생했습니다. path={}", filePath, e);
        }
    }
}