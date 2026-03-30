package com.dev.IbioScience.dto.seller.product.review;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DealerProductReviewAdminRowDto {

    private Long reviewId;
    private Long dealerProductId;

    /** Member.username */
    private String reviewerLoginId;

    /** 리뷰 엔티티에 저장된 회원 PK */
    private Long reviewerMemberId;

    /** 리뷰 엔티티에 저장된 표시명 */
    private String reviewerDisplayName;

    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String firstImageUrl;
    private int imageCount;
    private List<String> imageUrls = new ArrayList<>();

    public DealerProductReviewAdminRowDto(
            Long reviewId,
            Long dealerProductId,
            String reviewerLoginId,
            Long reviewerMemberId,
            String reviewerDisplayName,
            Integer rating,
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.reviewId = reviewId;
        this.dealerProductId = dealerProductId;
        this.reviewerLoginId = reviewerLoginId;
        this.reviewerMemberId = reviewerMemberId;
        this.reviewerDisplayName = reviewerDisplayName;
        this.rating = rating;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getMaskedReviewerId() {
        String source = null;

        if (reviewerLoginId != null && !reviewerLoginId.isBlank()) {
            source = reviewerLoginId;
        } else if (reviewerMemberId != null) {
            source = String.valueOf(reviewerMemberId);
        } else {
            source = "-";
        }

        if ("-".equals(source)) {
            return source;
        }

        if (source.length() <= 2) {
            return source;
        }

        return source.substring(0, 2) + "*".repeat(source.length() - 2);
    }

    public String getContentPreview() {
        if (content == null || content.isBlank()) {
            return "-";
        }

        String normalized = content.replace("\r\n", " ")
                                   .replace("\n", " ")
                                   .replace("\r", " ")
                                   .trim();

        if (normalized.length() <= 60) {
            return normalized;
        }

        return normalized.substring(0, 60) + "...";
    }

    public boolean isHasImages() {
        return imageCount > 0;
    }
}