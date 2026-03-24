package com.dev.IbioScience.dto.order;

import java.util.ArrayList;
import java.util.List;

import com.dev.IbioScience.model.product.ProductOption;
import com.dev.IbioScience.model.product.ProductOptionGroup;
import com.dev.IbioScience.model.product.dealer.DealerProductOption;
import com.dev.IbioScience.model.product.dealer.DealerProductOptionGroup;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductOptionGroupDto {

    private Long optionGroupId;
    private String groupName;

    // wishList.html에서 g.options 로 순회
    private List<ProductOptionDto> options;

    /**
     * 우리회사 상품 옵션그룹 -> DTO
     *
     * @param group     ProductOptionGroup 엔티티
     * @param basePrice 상품의 salePrice
     */
    public static ProductOptionGroupDto from(ProductOptionGroup group, Integer basePrice) {
        if (group == null) {
            return null;
        }

        List<ProductOptionDto> optionDtos = new ArrayList<>();
        if (group.getOptions() != null) {
            for (ProductOption opt : group.getOptions()) {
                optionDtos.add(ProductOptionDto.from(opt, basePrice));
            }
        }

        return ProductOptionGroupDto.builder()
                .optionGroupId(group.getId())
                .groupName(group.getName())
                .options(optionDtos)
                .build();
    }

    /**
     * 딜러 상품 옵션그룹 -> DTO
     *
     * @param group     DealerProductOptionGroup 엔티티
     * @param basePrice 상품의 salePrice
     */
    public static ProductOptionGroupDto fromDealer(DealerProductOptionGroup group, Integer basePrice) {
        if (group == null) {
            return null;
        }

        List<ProductOptionDto> optionDtos = new ArrayList<>();
        if (group.getOptions() != null) {
            for (DealerProductOption opt : group.getOptions()) {
                optionDtos.add(ProductOptionDto.fromDealer(opt, basePrice));
            }
        }

        return ProductOptionGroupDto.builder()
                .optionGroupId(group.getId())
                .groupName(group.getName())
                .options(optionDtos)
                .build();
    }
}