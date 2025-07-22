package com.dev.IbioScience.model.product;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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

// 내부 중분류
@Data
@Entity
@Table(name = "tb_internal_category_medium",
       uniqueConstraints = @UniqueConstraint(columnNames = {"large_id", "name"})) // 동일 대분류 내 중분류명 유니크
public class InternalCategoryMedium {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 내부 중분류명 (NOT NULL)
    @Column(nullable = false, length = 100)
    private String name;

    // 대분류 FK (NOT NULL)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "large_id", nullable = false)
    private InternalCategoryLarge large;

    // 내부 소분류 리스트 (1:N)
    @OneToMany(mappedBy = "medium", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InternalCategorySmall> smalls = new ArrayList<>();
}