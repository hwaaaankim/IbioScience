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

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DealerProductCreateRequest {

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

    private List<CategoryMappingRequest> categoryMappings = new ArrayList<>();
    private List<ExtraFieldRequest> extraFields = new ArrayList<>();
    private List<String> keywords = new ArrayList<>();
    private List<OptionGroupRequest> optionGroups = new ArrayList<>();

    @Getter
    @Setter
    public static class CategoryMappingRequest {
        private Long mediumId;
        private Long smallId;
    }

    @Getter
    @Setter
    public static class ExtraFieldRequest {
        private String label;
        private String value;
    }

    @Getter
    @Setter
    public static class OptionGroupRequest {
        private String name;
        private Integer sortOrder;
        private List<OptionRequest> options = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class OptionRequest {
        private String name;
        private String value;
        private BigDecimal extraPrice;
        private PriceSign sign;
        private Integer sortOrder;
    }
}