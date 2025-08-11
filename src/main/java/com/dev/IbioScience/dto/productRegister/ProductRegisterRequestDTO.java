package com.dev.IbioScience.dto.productRegister;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class ProductRegisterRequestDTO {
    private String productName;
    private String productCode;
    private String displayStatus;
    private String saleStatus;
    private String detailHtml;

    private List<Long> categorySmallIds = new ArrayList<>();

    private MultipartFile mainImage;
    private List<MultipartFile> subImages = new ArrayList<>();

    private String manufacturerText;
    private String supplierText;
    private Long brandId;

    private LocalDate manufacturedAt;
    private LocalDate expiredAt;

    private String summaryDescription;
    private String shortDescription;
    private String internalProductCode;

    private Integer consumerPrice;
    private Integer salePrice;

    private String priceExposeTarget;
    private Boolean usePriceReplacementText;
    private String priceReplacementText;

    private Float rewardRate;
    private LocalDate validFrom;
    private LocalDate validTo;

    private Boolean useRelatedProducts;
    private Boolean useBundleItems;

    private Long internalCategorySmallId;

    private String newState;

    private MultipartFile iconImage;
    private Boolean useIconPeriod;
    private LocalDate iconStartDate;
    private LocalDate iconEndDate;

    @Data
    public static class ExtraFieldDTO {
        private String label;
        private String value;
    }
    private List<ExtraFieldDTO> extraFields = new ArrayList<>();

    @Data
    public static class OptionDTO {
        private String name;
        private String value;
        private String extraPrice;
        private String sign;
        private Integer sortOrder;
    }
    @Data
    public static class OptionGroupDTO {
        private String name;
        private Integer sortOrder;
        private List<OptionDTO> options = new ArrayList<>();
    }
    private List<OptionGroupDTO> optionGroups = new ArrayList<>();

    private List<String> keywords = new ArrayList<>();

    @Data
    public static class RelatedProductDTO {
        private Long id;
        private String type;
        private Integer sortOrder;
    }
    private List<RelatedProductDTO> relatedProducts = new ArrayList<>();

    @Data
    public static class BundleProductDTO {
        private Long id;
        private Integer sortOrder;
    }
    private List<BundleProductDTO> bundleProducts = new ArrayList<>();

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

    private Map<String, String> dealerDiscounts = new HashMap<>();

    private Map<String, String> displayOptions = new HashMap<>();
    private Map<String, MultipartFile> displayOptionFiles = new HashMap<>();
}