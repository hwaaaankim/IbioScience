package com.dev.IbioScience.dto.productDetail;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.dev.IbioScience.dto.ProductQuestionApiDTO;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDetailReadResponseDTO {

    private Long id;

    // 기본정보
    private String displayStatus; // ON/OFF
    private String saleStatus;    // ON/OFF
    private String name;
    private String code;

    private String summaryDescription;
    private String shortDescription;
    private Integer consumerPrice;
    private Integer salePrice;
    private Float rewardRate;
    private String validFrom; // yyyy-MM-dd
    private String validTo;
    private String newState;  // NEW/STOCK/DISPLAY

    private String manufacturerText;
    private String supplierText;
    private String internalProductCode;
    private String manufacturedAt; // yyyy-MM-dd
    private String expiredAt;

    // 가격정책
    private PricePolicyReadDTO pricePolicy;

    // 분류
    private List<CategoryPathReadDTO> externalCategories; // (대/중/소)
    
    // ✅ 내부 자체분류 (대/중/소 모두 포함)
    private Long internalCategoryLargeId;
    private Long internalCategoryMediumId;
    private Long internalCategorySmallId;

    // 브랜드
    private BrandReadDTO brand;

    // 키워드
    private List<String> keywords;

    // 공통질문 정의 + 답변(분리)
    private List<ProductQuestionApiDTO> displayQuestions; // 주신 포맷 그대로
    private List<ProductAnswerReadDTO> answers;

    // 상세설명 HTML
    private String detailHtml;

    // 이미지
    private ImagesReadDTO images;
    private IconReadDTO icon;

    // 옵션
    private List<OptionGroupReadDTO> optionGroups;

    // 연관/번들
    private List<RelatedProductReadDTO> relatedProducts;
    private List<BundleProductReadDTO> bundleProducts;

    // 딜러 등급별 추가할인: 등급코드 → 할인율(%)
    private Map<String, BigDecimal> dealerDiscounts;

    // 프로모션(라벨 포함)
    private List<PromotionReadDTO> discounts;

    private List<ExtraFieldReadDTO> extraFields;

    @Data
    public static class ExtraFieldReadDTO {
        private String label;
        private String value;
    }
    
    @Data
    public static class PricePolicyReadDTO {
        private String priceExposeTarget;       // MEMBER/GUEST
        private Boolean usePriceReplacementText;
        private String priceReplacementText;
    }

    @Data
    public static class CategoryPathReadDTO {
        private Long largeId;    private String largeName;
        private Long mediumId;   private String mediumName;
        private Long smallId;    private String smallName;
    }

    @Data
    public static class BrandReadDTO {
        private Long id;
        private String name;
        private String imageUrl;
    }

    @Data
    public static class ImagesReadDTO {
        private String mainImageUrl;
        private List<String> subImageUrls;
    }

    @Data
    public static class IconReadDTO {
        private String imageUrl;       // 공개 URL
        private Boolean usePeriod;
        private String startDate;      // yyyy-MM-dd
        private String endDate;
    }

    @Data
    public static class OptionGroupReadDTO {
        private String name;
        private List<OptionReadDTO> options;
    }

    @Data
    public static class OptionReadDTO {
        private String name;
        private String value;
        private BigDecimal extraPrice;
        private String sign;       // PLUS/MINUS
        private Integer sortOrder;
    }

    @Data
    public static class RelatedProductReadDTO {
        private Long id;
        private String name;
        private Integer sortOrder;
        private String type;       // RECIPROCAL/ONEWAY
    }

    @Data
    public static class BundleProductReadDTO {
        private Long id;
        private String name;
        private Integer sortOrder;
    }

    @Data
    public static class PromotionReadDTO {
        private Long id;
        private String name;
        private String type;       // DISCOUNT/GIFT/ONE_PLUS_ONE/COUPON...
        private String term;       // PERIOD/ALWAYS...
        private Boolean active;
        private String startDate;  // yyyy-MM-dd
        private String endDate;
        private String target;     // PRODUCT/BRAND/CATEGORY...
        private String couponPolicy; // 필요시
        private String typeLabel;  // 서버에서 라벨 제공
        private String termLabel;
    }

    @Data
    public static class ProductAnswerReadDTO {
        private Long questionId;
        private String type;            // 질문 타입 (INPUT/TEXTAREA/SELECT/FILE/CKEDITOR)
        private String value;           // 텍스트/CKEditor HTML 답변
        private List<String> fileUrls;  // FILE일 때 기존 파일 URL 목록
    }
}