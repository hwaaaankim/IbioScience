package com.dev.IbioScience.model.product.relation;

import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
@Table(
    name = "tb_medium_small_product_category",
    uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "medium_id", "small_id"})
)
public class MediumSmallProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 제품
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 중분류
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medium_id", nullable = false)
    private CategoryMedium medium;

    // 소분류
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "small_id", nullable = false)
    private CategorySmall small;
}