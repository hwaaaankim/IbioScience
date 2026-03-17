package com.dev.IbioScience.dto.estimate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstimateProductRowDto {

    private Long mappingId;
    private Long productId;

    private Long largeId;
    private String largeName;

    private Long mediumId;
    private String mediumName;

    private Long smallId;
    private String smallName;

    private Long brandId;
    private String brandName;

    private String productName;
    private String productCode;

    /** 서비스에서 대표이미지 채움 */
    private String imageUrl;

    public EstimateProductRowDto(
            Long mappingId,
            Long productId,
            Long largeId,
            String largeName,
            Long mediumId,
            String mediumName,
            Long smallId,
            String smallName,
            Long brandId,
            String brandName,
            String productName,
            String productCode
    ) {
        this.mappingId = mappingId;
        this.productId = productId;
        this.largeId = largeId;
        this.largeName = largeName;
        this.mediumId = mediumId;
        this.mediumName = mediumName;
        this.smallId = smallId;
        this.smallName = smallName;
        this.brandId = brandId;
        this.brandName = brandName;
        this.productName = productName;
        this.productCode = productCode;
    }
}