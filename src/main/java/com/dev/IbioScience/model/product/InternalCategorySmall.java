package com.dev.IbioScience.model.product;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

// 내부 소분류
@Data
@Entity
@Table(name = "tb_internal_category_small",
       uniqueConstraints = @UniqueConstraint(columnNames = {"medium_id", "name"})) // 동일 중분류 내 소분류명 유니크
public class InternalCategorySmall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 내부 소분류명 (NOT NULL)
    @Column(nullable = false, length = 100)
    private String name;

    // 중분류 FK (NOT NULL)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medium_id", nullable = false)
    private InternalCategoryMedium medium;

    // 소분류-제품 리스트 (1:N)
    @OneToMany(mappedBy = "internalCategorySmall")
    @JsonManagedReference("product-internal-category")
    private List<Product> products = new ArrayList<>();
}