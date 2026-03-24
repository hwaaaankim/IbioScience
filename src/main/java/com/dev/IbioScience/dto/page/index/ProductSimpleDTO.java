package com.dev.IbioScience.dto.page.index;

import com.dev.IbioScience.enums.product.dealer.ProductSourceType;
import com.dev.IbioScience.helper.index.FrontProductRouteHelper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 메뉴/카테고리 교집합용 제품 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSimpleDTO {

    private Long id;
    private String name;
    private Long brandId;

    private ProductSourceType productSourceType;
    private String productSourceLabel;
    private String productKey;
    private String detailUrl;

    /**
     * 기존 우리회사 제품 query 호환 생성자
     */
    public ProductSimpleDTO(Long id, String name, Long brandId) {
        this.id = id;
        this.name = name;
        this.brandId = brandId;
        applyCompanyMetadata();
    }

    public void applyCompanyMetadata() {
        this.productSourceType = ProductSourceType.COMPANY;
        this.productSourceLabel = ProductSourceType.COMPANY.getLabel();
        this.productKey = FrontProductRouteHelper.buildProductKey(ProductSourceType.COMPANY, this.id);
        this.detailUrl = FrontProductRouteHelper.buildDetailUrl(ProductSourceType.COMPANY, this.id);
    }

    public static ProductSimpleDTO fromDealer(Long id, String name) {
        ProductSimpleDTO dto = new ProductSimpleDTO();
        dto.id = id;
        dto.name = name;
        dto.brandId = null;
        dto.productSourceType = ProductSourceType.DEALER;
        dto.productSourceLabel = ProductSourceType.DEALER.getLabel();
        dto.productKey = FrontProductRouteHelper.buildProductKey(ProductSourceType.DEALER, id);
        dto.detailUrl = FrontProductRouteHelper.buildDetailUrl(ProductSourceType.DEALER, id);
        return dto;
    }
}