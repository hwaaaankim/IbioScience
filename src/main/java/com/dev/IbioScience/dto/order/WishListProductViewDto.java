package com.dev.IbioScience.dto.order;

import java.util.ArrayList;
import java.util.List;

import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.ProductOptionGroup;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WishListProductViewDto {

    private Long productId;
    private String productName;
    private String brandName;

    private Integer salePrice;     // tb_product.salePrice
    private Integer consumerPrice; // tb_product.consumerPrice
    private String mainImageUrl;   // 대표(MAIN) 이미지 url

    private int optionCount;       // 옵션 총 개수
    private List<ProductOptionGroupDto> optionGroups;

    /**
     * @param product      Product 엔티티
     * @param groups       옵션 그룹(각 그룹의 options 포함되어 있어야 함)
     * @param mainImageUrl 대표이미지 url (없으면 null)
     */
    public static WishListProductViewDto from(Product product,
                                              List<ProductOptionGroup> groups,
                                              String mainImageUrl) {
        if (product == null) return null;

        String brandName = null;
        if (product.getBrand() != null) {
            // Brand 엔티티의 필드명이 name이 맞다는 가정입니다.
            // 만약 brand명이 다른 필드면 Brand 엔티티 코드 보내주시면 정확히 맞춰드립니다.
            brandName = product.getBrand().getName();
        }

        Integer basePrice = product.getSalePrice();

        List<ProductOptionGroupDto> groupDtos = new ArrayList<>();
        int count = 0;

        if (groups != null) {
            for (ProductOptionGroup g : groups) {
                ProductOptionGroupDto dto = ProductOptionGroupDto.from(g, basePrice);
                groupDtos.add(dto);

                if (dto.getOptions() != null) {
                    count += dto.getOptions().size();
                }
            }
        }

        return WishListProductViewDto.builder()
                .productId(product.getId())
                .productName(product.getName())
                .brandName(brandName)
                .salePrice(product.getSalePrice())
                .consumerPrice(product.getConsumerPrice())
                .mainImageUrl(mainImageUrl)
                .optionCount(count)
                .optionGroups(groupDtos)
                .build();
    }
}