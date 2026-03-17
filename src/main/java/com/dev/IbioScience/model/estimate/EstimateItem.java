package com.dev.IbioScience.model.estimate;

import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.relation.MediumSmallProductCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tb_estimate_item")
public class EstimateItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 견적 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimate_id", nullable = false)
    private Estimate estimate;

    /** 실제 선택된 카테고리-상품 매핑 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mapping_id", nullable = false)
    private MediumSmallProductCategory mapping;

    /** 실제 제품 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** 수량 */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** 아래는 스냅샷 */
    @Column(name = "large_category_name", nullable = false, length = 100)
    private String largeCategoryName;

    @Column(name = "medium_category_name", nullable = false, length = 100)
    private String mediumCategoryName;

    @Column(name = "small_category_name", nullable = false, length = 100)
    private String smallCategoryName;

    @Column(name = "brand_name", length = 100)
    private String brandName;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "product_code", length = 100)
    private String productCode;
}