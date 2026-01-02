package com.dev.IbioScience.dto.order;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.dev.IbioScience.enums.product.PriceSign;
import com.dev.IbioScience.model.product.ProductOption;
import com.dev.IbioScience.model.product.ProductOptionGroup;

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
    private String optionCode;   // opt.optionCode  (== value)
    private Integer finalPrice;  // opt.finalPrice  (상품 salePrice +/- extraPrice)

    /**
     * @param opt       ProductOption 엔티티
     * @param basePrice 상품의 salePrice (null이면 0)
     */
    public static ProductOptionDto from(ProductOption opt, Integer basePrice) {
        if (opt == null) return null;

        Integer computedFinalPrice = computeFinalPrice(basePrice, opt.getExtraPrice(), opt.getSign());

        ProductOptionGroup g = opt.getGroup();
        Long groupId = (g != null ? g.getId() : null);
        String groupName = (g != null ? g.getName() : null);

        return ProductOptionDto.builder()
                .optionId(opt.getId())
                .optionGroupId(groupId)
                .optionGroupName(groupName)
                .optionName(opt.getName())
                .optionCode(opt.getValue()) // ✅ value를 CAT.NO(코드)로 사용
                .finalPrice(computedFinalPrice)
                .build();
    }

    private static Integer computeFinalPrice(Integer basePrice, BigDecimal extraPrice, PriceSign sign) {
        int base = (basePrice == null ? 0 : basePrice.intValue());

        BigDecimal extra = (extraPrice == null ? BigDecimal.ZERO : extraPrice);

        // extraPrice가 소수일 수도 있으니 원단위 반올림
        int extraInt = extra.setScale(0, RoundingMode.HALF_UP).intValue();

        if (sign == null || sign == PriceSign.PLUS) {
            return base + extraInt;
        }
        if (sign == PriceSign.MINUS) {
            return base - extraInt;
        }
        // 혹시 enum이 확장되면 기본은 PLUS 처리
        return base + extraInt;
    }
}