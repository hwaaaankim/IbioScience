package com.dev.IbioScience.dto.seller.product;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SellerProductFormMetaResponse {

    private String shopName;
    private List<EnumOption> displayStatuses;
    private List<EnumOption> saleStatuses;
    private List<EnumOption> productStates;
    private List<EnumOption> newStates;
    private List<EnumOption> priceExposeTargets;
    private List<EnumOption> priceSigns;
    private List<LargeNode> allowedCategories;

    @Getter
    @Setter
    @Builder
    public static class EnumOption {
        private String value;
        private String label;
    }

    @Getter
    @Setter
    @Builder
    public static class LargeNode {
        private Long id;
        private String name;

        @Builder.Default
        private List<MediumNode> mediums = new ArrayList<>();
    }

    @Getter
    @Setter
    @Builder
    public static class MediumNode {
        private Long id;
        private String name;

        @Builder.Default
        private List<SmallNode> smalls = new ArrayList<>();
    }

    @Getter
    @Setter
    @Builder
    public static class SmallNode {
        private Long id;
        private String name;
    }
}