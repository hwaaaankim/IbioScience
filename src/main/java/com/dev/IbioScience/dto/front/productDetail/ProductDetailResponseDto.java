package com.dev.IbioScience.dto.front.productDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponseDto {

    // ===== 기본 정보 =====
    private Long id;
    private String name;
    private String code;

    private String displayStatus;       // 예: "ON"
    private String displayStatusLabel;  // 예: "진열함"

    private String saleStatus;          // 예: "ON"
    private String saleStatusLabel;     // 필요시

    private String productState;        // 예: "NORMAL"
    private String productStateLabel;   // 예: "정상"

    private String newState;            // 예: "NEW"
    private String newStateLabel;       // 예: "신상품"

    private Integer salesCount;
    private Integer viewCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String manufacturerText;
    private String supplierText;
    private LocalDate manufacturedAt;
    private LocalDate expiredAt;

    private String summaryDescription;
    private String shortDescription;
    private String detailHtml;

    // ===== 가격 정보 =====
    private Integer consumerPrice;
    private Integer salePrice;

    private String priceExposeTarget;        // 예: "MEMBER", "GUEST"
    private Boolean usePriceReplacementText;
    private String priceReplacementText;

    // ===== 아이콘 정보 =====
    private String iconUrl;
    private String iconPath;
    private String iconFileName;
    private Boolean useIconPeriod;
    private LocalDate iconStartDate;
    private LocalDate iconEndDate;

    // ===== 브랜드 =====
    private BrandDto brand;

    // ===== 카테고리 (대/중/소) N:N 구조 =====
    private List<CategoryPathDto> categories;

    // ===== 이미지 =====
    private List<ProductImageDto> images;
    private List<ProductDetailImageDto> detailImages;

    // ===== 옵션/추가필드 =====
    private List<OptionGroupDto> optionGroups;
    private List<ExtraFieldDto> extraFields;

    // ===== 번들/연관상품 =====
    private List<BundleItemDto> bundleItems;
    private List<RelatedProductDto> relatedProducts;

    // ===== 프로모션 / 쿠폰 / 등급혜택 =====
    private List<PromotionDto> promotions;
    private List<GradeBenefitDto> gradeBenefits;

    // ===== 키워드 =====
    private List<KeywordDto> keywords;

    // ===== 리뷰 =====
    private ReviewSummaryDto reviewSummary;
    private List<ReviewDto> reviews;

    // ===== 공통질문/답변 블럭 =====
    private List<QuestionBlockDto> questions;

    // ===== 가격 노출 예시 (인증 정보가 없으므로 예시) =====
    private PricePreviewExampleDto pricePreviewExample;

    // ========== 내부 static DTO들 ==========

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BrandDto {
        private Long id;
        private String name;
        private String imageUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryPathDto {
        private Long largeId;
        private String largeName;
        private Long mediumId;
        private String mediumName;
        private Long smallId;
        private String smallName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductImageDto {
        private Long id;
        private String type;       // "MAIN", "ADDITIONAL"
        private String url;
        private String path;
        private String fileName;
        private Integer sortOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductDetailImageDto {
        private Long id;
        private String url;
        private String path;
        private String fileName;
        private String originalFilename;
        private Integer size;
        private LocalDateTime uploadedAt;
        private Boolean inUse;
        private Integer sortOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionGroupDto {
        private Long id;
        private String name;
        private Integer sortOrder;
        private List<OptionDto> options;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionDto {
        private Long id;
        private String name;
        private String value;
        private BigDecimal extraPrice;
        private String sign;       // "PLUS", "MINUS"
        private String signLabel;  // "추가", "차감"
        private Integer sortOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExtraFieldDto {
        private Long id;
        private String label;
        private String value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BundleItemDto {
        private Long id;
        private Long bundleProductId;
        private String bundleProductName;
        private Integer sortOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RelatedProductDto {
        private Long id;
        private Long relatedProductId;
        private String relatedProductName;
        private String relatedType;      // "ONEWAY", "RECIPROCAL"
        private String relatedTypeLabel; // "단방향", "쌍방향"
        private Integer sortOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PromotionDto {
        private Long id;
        private String name;
        private Boolean conditionEnabled;
        private String iconUrl;
        private String iconPath;
        private Boolean active;
        private String type;        // DISCOUNT/GIFT/...
        private String typeLabel;
        private String term;        // PERIOD/ALWAYS
        private String termLabel;
        private LocalDate startDate;
        private LocalDate endDate;
        private String couponName;
        private BigDecimal discountPercent;
        private Long giftProductId;
        private String giftProductName;
        private String target;      // NORMAL 등
        private String targetLabel;
        private Long couponId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GradeBenefitDto {
        private Long id;
        private String dealerGrade;       // "A", "B" ...
        private BigDecimal discountRate;  // 할인율(%)
        private Integer examplePrice;     // 예시 계산된 판매가 (있으면)
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KeywordDto {
        private Long id;
        private String word;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewSummaryDto {
        private Double averageRating;
        private Long reviewCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewImageDto {
        private Long id;
        private String url;
        private String path;
        private String fileName;
        private String originalFilename;
        private Integer size;
        private Integer sortOrder;
        private LocalDateTime uploadedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewDto {
        private Long id;
        private Long memberId;
        private String memberDisplayName;
        private Integer rating;
        private String content;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<ReviewImageDto> images;
    }

    /**
     * 공통 질문/옵션/답변(이미지 포함) 블럭
     * - 질문 1개 + (선택지 옵션들) + (해당 제품의 답변들) 구조
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionBlockDto {
        private Long questionId;
        private String label;
        private String type;       // QuestionType.name()
        private String typeLabel;  // QuestionType.getLabel()
        private Boolean required;
        private String placeholder;
        private Integer sortOrder;

        private List<QuestionOptionDto> options;  // SELECT 등 선택지
        private List<AnswerDto> answers;          // 해당 제품의 답변들
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionOptionDto {
        private Long id;
        private String value;
        private Integer sortOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnswerDto {
        private Long id;
        private String value;          // 텍스트/HTML
        private String fileUrl;        // 파일 타입일 경우
        private String path;
        private String fileName;
        private List<AnswerDetailImageDto> detailImages;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnswerDetailImageDto {
        private Long id;
        private String url;
        private String path;
        private String fileName;
        private String originalFilename;
        private Integer size;
        private LocalDateTime uploadedAt;
        private Boolean inUse;
        private Integer sortOrder;
    }

    /**
     * 인증 정보 없이 가격 노출 예시 제공용 DTO
     * - 일반회원 기준
     * - 딜러 A 등급 기준 (존재 시)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PricePreviewExampleDto {
        private boolean priceVisibleForGuest;
        private boolean priceVisibleForMember;

        private Integer baseSalePrice;          // product.salePrice
        private Integer exampleNormalMemberPrice;
        private Integer exampleDealerAPrice;

        private boolean useReplacementText;
        private String replacementText;

        private String description; // "현재는 로그인 정보가 없어 예시 가격을 내려줍니다..." 등 설명
    }
}