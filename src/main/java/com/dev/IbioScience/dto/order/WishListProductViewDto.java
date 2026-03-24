package com.dev.IbioScience.dto.order;

import java.util.ArrayList;
import java.util.List;

import com.dev.IbioScience.enums.product.dealer.WishListProductType;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.ProductOptionGroup;
import com.dev.IbioScience.model.product.dealer.DealerProduct;
import com.dev.IbioScience.model.product.dealer.DealerProductOptionGroup;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WishListProductViewDto {

    /** 기존 호환용 ID 필드 */
    private Long productId;

    /** 실제 관심상품 대상 ID (회사상품이면 product.id, 딜러상품이면 dealerProduct.id) */
    private Long targetId;

    /** 화면/DOM 식별용 고유 키 (예: COMPANY_3, DEALER_15) */
    private String rowKey;

    /** 상품 타입 */
    private WishListProductType productType;

    /** 화면 표시용 상품 타입 라벨 */
    private String productTypeLabel;

    /** 상품명 */
    private String productName;

    /** 브랜드명 또는 상점명 */
    private String brandName;

    /** 판매가 */
    private Integer salePrice;

    /** 소비자가 */
    private Integer consumerPrice;

    /** 대표 이미지 URL */
    private String mainImageUrl;

    /** 옵션 총 개수 */
    private int optionCount;

    /** 옵션 그룹 */
    private List<ProductOptionGroupDto> optionGroups;

    /** 상세페이지 이동 URL */
    private String detailUrl;

    /** 옵션 패널 활성화 여부 */
    private boolean optionPanelEnabled;

    public static WishListProductViewDto from(
            Product product,
            List<ProductOptionGroup> groups,
            String mainImageUrl
    ) {
        if (product == null) {
            return null;
        }

        String brandName = null;
        if (product.getBrand() != null) {
            brandName = product.getBrand().getName();
        }

        Integer basePrice = product.getSalePrice();

        List<ProductOptionGroupDto> groupDtos = new ArrayList<>();
        int count = 0;

        if (groups != null) {
            for (ProductOptionGroup group : groups) {
                ProductOptionGroupDto groupDto = ProductOptionGroupDto.from(group, basePrice);
                groupDtos.add(groupDto);

                if (groupDto.getOptions() != null) {
                    count += groupDto.getOptions().size();
                }
            }
        }

        Long productId = product.getId();

        return WishListProductViewDto.builder()
                .productId(productId)
                .targetId(productId)
                .rowKey(buildRowKey(WishListProductType.COMPANY, productId))
                .productType(WishListProductType.COMPANY)
                .productTypeLabel("우리회사제품")
                .productName(product.getName())
                .brandName(brandName)
                .salePrice(product.getSalePrice())
                .consumerPrice(product.getConsumerPrice())
                .mainImageUrl(mainImageUrl)
                .optionCount(count)
                .optionGroups(groupDtos)
                .detailUrl("/productDetail/" + productId)
                .optionPanelEnabled(count > 0)
                .build();
    }

    public static WishListProductViewDto fromDealerProduct(
            DealerProduct dealerProduct,
            List<DealerProductOptionGroup> groups,
            String mainImageUrl,
            String detailUrl
    ) {
        if (dealerProduct == null) {
            return null;
        }

        String brandName = null;
        if (dealerProduct.getSellerDealerProfile() != null) {
            brandName = dealerProduct.getSellerDealerProfile().getShopName();
        }

        Integer basePrice = dealerProduct.getSalePrice();

        List<ProductOptionGroupDto> groupDtos = new ArrayList<>();
        int count = 0;

        if (groups != null) {
            for (DealerProductOptionGroup group : groups) {
                ProductOptionGroupDto groupDto = ProductOptionGroupDto.fromDealer(group, basePrice);
                groupDtos.add(groupDto);

                if (groupDto.getOptions() != null) {
                    count += groupDto.getOptions().size();
                }
            }
        }

        Long dealerProductId = dealerProduct.getId();

        return WishListProductViewDto.builder()
                .productId(dealerProductId)
                .targetId(dealerProductId)
                .rowKey(buildRowKey(WishListProductType.DEALER, dealerProductId))
                .productType(WishListProductType.DEALER)
                .productTypeLabel("딜러제품")
                .productName(dealerProduct.getName())
                .brandName(brandName)
                .salePrice(dealerProduct.getSalePrice())
                .consumerPrice(dealerProduct.getConsumerPrice())
                .mainImageUrl(mainImageUrl)
                .optionCount(count)
                .optionGroups(groupDtos)
                .detailUrl(detailUrl)
                .optionPanelEnabled(count > 0)
                .build();
    }

    private static String buildRowKey(WishListProductType productType, Long targetId) {
        return productType.name() + "_" + targetId;
    }
}