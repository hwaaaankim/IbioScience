package com.dev.IbioScience.model.product.dealer;

import com.dev.IbioScience.enums.product.ProductImageType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "tb_dealer_product_image",
    indexes = {
        @Index(name = "ix_dealer_product_image_product", columnList = "dealer_product_id"),
        @Index(name = "ix_dealer_product_image_sort", columnList = "sort_order")
    }
)
public class DealerProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 딜러상품 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_product_id", nullable = false)
    private DealerProduct dealerProduct;

    /** 이미지 타입(MAIN, ADDITIONAL) */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private ProductImageType type;

    /** 공개 URL */
    @Column(name = "url", length = 500)
    private String url;

    /** 저장 PATH */
    @Column(name = "path", length = 500)
    private String path;

    /** 파일명 */
    @Column(name = "file_name", length = 255)
    private String fileName;

    /** 정렬순서 */
    @Column(name = "sort_order")
    private Integer sortOrder;
}