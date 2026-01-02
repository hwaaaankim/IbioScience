package com.dev.IbioScience.dto.order;

import java.util.ArrayList;
import java.util.List;

import com.dev.IbioScience.model.product.ProductOption;
import com.dev.IbioScience.model.product.ProductOptionGroup;

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
     * @param group     ProductOptionGroup 엔티티
     * @param basePrice 상품의 salePrice
     */
    public static ProductOptionGroupDto from(ProductOptionGroup group, Integer basePrice) {
        if (group == null) return null;

        List<ProductOptionDto> optionDtos = new ArrayList<>();
        if (group.getOptions() != null) {
            for (ProductOption opt : group.getOptions()) {
                optionDtos.add(ProductOptionDto.from(opt, basePrice));
            }
        }

        return ProductOptionGroupDto.builder()
                .optionGroupId(group.getId())
                .groupName(group.getName()) // ✅ name 사용
                .options(optionDtos)
                .build();
    }
}