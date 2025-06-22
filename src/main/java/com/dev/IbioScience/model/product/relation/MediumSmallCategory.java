package com.dev.IbioScience.model.product.relation;

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
    name = "tb_medium_small_category",
    uniqueConstraints = @UniqueConstraint(columnNames = {"medium_id", "small_id"})
)
public class MediumSmallCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medium_id", nullable = false)
    private CategoryMedium medium;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "small_id", nullable = false)
    private CategorySmall small;

    private Integer sortOrder;
}

