package com.dev.IbioScience.model.product;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

// 내부 대분류
@Data
@Entity
@Table(name = "tb_internal_category_large",
       uniqueConstraints = @UniqueConstraint(columnNames = "name")) // 내부 대분류명 유니크
public class InternalCategoryLarge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 내부 대분류명 (유니크, NOT NULL)
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // 내부 중분류 리스트 (1:N)
    @OneToMany(mappedBy = "large", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InternalCategoryMedium> mediums = new ArrayList<>();
}