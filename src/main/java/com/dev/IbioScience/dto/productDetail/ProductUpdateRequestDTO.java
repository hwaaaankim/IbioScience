package com.dev.IbioScience.dto.productDetail;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

@Data
public class ProductUpdateRequestDTO {

    // ===== 기본 정보 =====
    private String productName;
    private String productCode;
    private String displayStatus;
    private String saleStatus;
    private String detailHtml;

    // 카테고리(외부)
    @Data
    public static class ExternalCategoryDTO {
        private Long mediumId;
        private Long smallId;
    }
    private List<ExternalCategoryDTO> externalCategories = new ArrayList<>();
    
    // 이미지(대표/추가)
    // 대표 이미지 액션: KEEP | DELETE | REPLACE
    private String mainImageAction;
    private MultipartFile mainImage;

    // 추가 이미지: 삭제할 서버 URL 목록 + 신규 업로드
    private List<String> subImageDeleteUrls = new ArrayList<>();
    private List<MultipartFile> subImages = new ArrayList<>();

    // 제조/공급/브랜드/일자/요약
    private String manufacturerText;
    private String supplierText;
    private Long brandId;
    private LocalDate manufacturedAt;
    private LocalDate expiredAt;
    private String summaryDescription;
    private String shortDescription;
    private String internalProductCode;

    // 가격/정책
    private Integer consumerPrice;
    private Integer salePrice;
    private String priceExposeTarget;
    private Boolean usePriceReplacementText;
    private String priceReplacementText;
    private Float rewardRate;
    private LocalDate validFrom;
    private LocalDate validTo;

    // 관련/번들 사용 여부
    private Boolean useRelatedProducts;
    private Boolean useBundleItems;

    // 내부 카테고리(자체)
    private Long internalCategorySmallId;

    // 신상품 상태
    private String newState;

    // ===== 아이콘 =====
    // 아이콘 이미지 액션: KEEP | DELETE | REPLACE
    private String iconImageAction;
    private MultipartFile iconImage;
    private Boolean useIconPeriod;
    private LocalDate iconStartDate;
    private LocalDate iconEndDate;

    // ===== 추가 입력필드 =====
    @Data
    public static class ExtraFieldDTO {
        private String label;
        private String value;
    }
    private List<ExtraFieldDTO> extraFields = new ArrayList<>();

    // ===== 옵션 =====
    @Data
    public static class OptionDTO {
        private String name;
        private String value;
        private String extraPrice; // 빈문자 허용
        private String sign;       // PLUS | MINUS
        private Integer sortOrder;
    }
    @Data
    public static class OptionGroupDTO {
        private String name;
        private Integer sortOrder;
        private List<OptionDTO> options = new ArrayList<>();
    }
    private List<OptionGroupDTO> optionGroups = new ArrayList<>();

    // ===== 키워드 =====
    private List<String> keywords = new ArrayList<>();

    // ===== 관련/번들 =====
    @Data
    public static class RelatedProductDTO {
        private Long id;
        private String type;     // RECIPROCAL | ONEWAY
        private Integer sortOrder;
    }
    private List<RelatedProductDTO> relatedProducts = new ArrayList<>();

    @Data
    public static class BundleProductDTO {
        private Long id;
        private Integer sortOrder;
    }
    private List<BundleProductDTO> bundleProducts = new ArrayList<>();

    // ===== 프로모션 =====
    @Data
    public static class DiscountDTO {
        private Long id;
        private String name;
        private String type;
        private String term;
        private String target;
        private String couponPolicy;
        private String startDate;
        private String endDate;
        private Boolean active;
    }
    private List<DiscountDTO> discounts = new ArrayList<>();

    // ===== 딜러 할인율 =====
    private Map<String, String> dealerDiscounts = new HashMap<>();

    // ===== 공통표시항목(질문/답변) =====
    // 텍스트/셀렉트/에디터 값
    private Map<String, String> displayOptions = new HashMap<>();
    // 파일 업로드(질문별)
    private Map<String, List<MultipartFile>> displayOptionFiles = new HashMap<>();
    // 파일형 질문 액션 key: "question_{id}_fileAction" value: KEEP|DELETE|REPLACE
    private Map<String, String> displayOptionFileActions = new HashMap<>();
}
