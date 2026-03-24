package com.dev.IbioScience.dto.order;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.dev.IbioScience.enums.product.PriceSign;
import com.dev.IbioScience.model.product.ProductOption;
import com.dev.IbioScience.model.product.ProductOptionGroup;
import com.dev.IbioScience.model.product.dealer.DealerProductOption;
import com.dev.IbioScience.model.product.dealer.DealerProductOptionGroup;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductOptionDto {

    private Long optionId;
    private Long optionGroupId;
    private String optionGroupName;

    // wishList.html에서 쓰는 필드명 그대로 맞춤
    private String optionName;   // opt.optionName
    private String optionCode;   // opt.optionCode
    private Integer finalPrice;  // opt.finalPrice

    /**
     * 우리회사 상품 옵션 -> DTO
     *
     * @param opt       ProductOption 엔티티
     * @param basePrice 상품의 salePrice
     */
    public static ProductOptionDto from(ProductOption opt, Integer basePrice) {
        if (opt == null) {
            return null;
        }

        Integer computedFinalPrice = computeFinalPrice(basePrice, opt.getExtraPrice(), opt.getSign());

        ProductOptionGroup g = opt.getGroup();
        Long groupId = (g != null ? g.getId() : null);
        String groupName = (g != null ? g.getName() : null);

        return ProductOptionDto.builder()
                .optionId(opt.getId())
                .optionGroupId(groupId)
                .optionGroupName(groupName)
                .optionName(opt.getName())
                .optionCode(opt.getValue())
                .finalPrice(computedFinalPrice)
                .build();
    }

    /**
     * 딜러 상품 옵션 -> DTO
     *
     * @param opt       DealerProductOption 엔티티
     * @param basePrice 딜러상품의 salePrice
     */
    public static ProductOptionDto fromDealer(DealerProductOption opt, Integer basePrice) {
        if (opt == null) {
            return null;
        }

        Integer computedFinalPrice = computeFinalPrice(basePrice, opt.getExtraPrice(), opt.getSign());

        DealerProductOptionGroup g = opt.getGroup();
        Long groupId = (g != null ? g.getId() : null);
        String groupName = (g != null ? g.getName() : null);

        return ProductOptionDto.builder()
                .optionId(opt.getId())
                .optionGroupId(groupId)
                .optionGroupName(groupName)
                .optionName(opt.getName())
                .optionCode(opt.getValue())
                .finalPrice(computedFinalPrice)
                .build();
    }

    private static Integer computeFinalPrice(Integer basePrice, BigDecimal extraPrice, PriceSign sign) {
        int base = (basePrice == null ? 0 : basePrice.intValue());

        BigDecimal extra = (extraPrice == null ? BigDecimal.ZERO : extraPrice);
        int extraInt = extra.setScale(0, RoundingMode.HALF_UP).intValue();

        if (sign == null || sign == PriceSign.PLUS) {
            return base + extraInt;
        }

        if (sign == PriceSign.MINUS) {
            return base - extraInt;
        }

        return base + extraInt;
    }
}