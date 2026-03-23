package com.dev.IbioScience.dto.seller.product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.PriceExposeTarget;
import com.dev.IbioScience.enums.product.PriceSign;
import com.dev.IbioScience.enums.product.ProductNewState;
import com.dev.IbioScience.enums.product.ProductState;
import com.dev.IbioScience.enums.product.SaleStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SellerProductDetailResponse {

    private Long id;

    private DisplayStatus displayStatus;
    private SaleStatus saleStatus;
    private ProductState state;
    private ProductNewState newState;

    private String name;
    private String code;
    private String manufacturerText;
    private String supplierText;
    private LocalDate manufacturedAt;
    private LocalDate expiredAt;

    private String detailHtml;
    private String summaryDescription;
    private String shortDescription;
    private String internalProductCode;

    private Integer consumerPrice;
    private Integer salePrice;
    private PriceExposeTarget priceExposeTarget;
    private Boolean usePriceReplacementText;
    private String priceReplacementText;
    private Float rewardRate;

    private LocalDate validFrom;
    private LocalDate validTo;

    private Boolean useIconPeriod;
    private LocalDate iconStartDate;
    private LocalDate iconEndDate;

    private SimpleImageResponse representativeImage;
    private SimpleImageResponse iconImage;
    private List<AdditionalImageResponse> additionalImages;

    private List<CategoryMappingResponse> categoryMappings;
    private List<ExtraFieldResponse> extraFields;
    private List<String> keywords;
    private List<OptionGroupResponse> optionGroups;

    @Getter
    @Builder
    public static class SimpleImageResponse {
        private String url;
        private String fileName;
    }

    @Getter
    @Builder
    public static class AdditionalImageResponse {
        private Long id;
        private String url;
        private String fileName;
        private Integer sortOrder;
    }

    @Getter
    @Builder
    public static class CategoryMappingResponse {
        private Long largeId;
        private String largeName;
        private Long mediumId;
        private String mediumName;
        private Long smallId;
        private String smallName;
    }

    @Getter
    @Builder
    public static class ExtraFieldResponse {
        private String label;
        private String value;
    }

    @Getter
    @Builder
    public static class OptionGroupResponse {
        private String name;
        private Integer sortOrder;
        @Builder.Default
        private List<OptionResponse> options = new ArrayList<>();
    }

    @Getter
    @Builder
    public static class OptionResponse {
        private String name;
        private String value;
        private BigDecimal extraPrice;
        private PriceSign sign;
        private Integer sortOrder;
    }
}