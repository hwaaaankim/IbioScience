package com.dev.IbioScience.dto.product.select;

import java.time.LocalDate;
import java.util.List;

import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.SaleStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductDetailDto {
    private Long id;
    private String name;
    private String code;
    private DisplayStatus displayStatus;
    private SaleStatus saleStatus;
    private String summaryDescription;
    private String shortDescription;
    private String detailHtml;
    private Integer salePrice;
    private Integer consumerPrice;
    private String brandName;
    private String iconUrl;
    private Boolean useIconPeriod;
    private LocalDate iconStartDate;
    private LocalDate iconEndDate;

    private String internalCategorySmallName;
    private String internalCategoryMediumName;
    private String internalCategoryLargeName;

    private List<ImageDto> images;
    private List<ImageDto> detailImages;

    private List<OptionGroupDto> optionGroups;
    private List<ExtraFieldDto> extraFields;

    private List<BundleItemDto> bundleItems;
    private List<RelatedProductDto> relatedProducts;

    private List<String> keywords;

    private Double averageRating;
    private Long reviewCount;

    @Data @Builder
    public static class ImageDto {
        private String url;
        private String path;
        private String fileName;
        private Integer sortOrder;
    }

    @Data @Builder
    public static class OptionDto {
        private String name;
        private String value;
        private String sign; // PLUS/MINUS
        private String extraPrice; // 문자열로 변환해 프론트 포맷팅 보조
        private Integer sortOrder;
    }

    @Data @Builder
    public static class OptionGroupDto {
        private String name;
        private Integer sortOrder;
        private List<OptionDto> options;
    }

    @Data @Builder
    public static class ExtraFieldDto {
        private String label;
        private String value;
    }

    @Data @Builder
    public static class BundleItemDto {
        private Long productId;
        private String productName;
        private Integer sortOrder;
    }

    @Data @Builder
    public static class RelatedProductDto {
        private Long productId;
        private String productName;
        private String type;
        private Integer sortOrder;
    }
}