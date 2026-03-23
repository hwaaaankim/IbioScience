package com.dev.IbioScience.model.product.dealer;

import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "tb_dealer_medium_small_product_category",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_dealer_mspc_product_medium_small",
            columnNames = {"dealer_product_id", "medium_id", "small_id"}
        )
    },
    indexes = {
        @Index(name = "ix_dealer_mspc_product", columnList = "dealer_product_id"),
        @Index(name = "ix_dealer_mspc_medium", columnList = "medium_id"),
        @Index(name = "ix_dealer_mspc_small", columnList = "small_id")
    }
)
public class DealerMediumSmallProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 딜러상품 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_product_id", nullable = false)
    private DealerProduct dealerProduct;

    /** 중분류 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medium_id", nullable = false)
    private CategoryMedium medium;

    /** 소분류 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "small_id", nullable = false)
    private CategorySmall small;
}