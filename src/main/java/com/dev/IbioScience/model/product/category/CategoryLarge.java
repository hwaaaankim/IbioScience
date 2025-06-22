package com.dev.IbioScience.model.product.category;

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

@Data
@Entity
@Table(
    name = "tb_category_large",
    uniqueConstraints = @UniqueConstraint(columnNames = "name") // 대분류명 유니크
)
public class CategoryLarge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 대분류명 (유니크)
    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "large", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CategoryMedium> mediums = new ArrayList<>();
}
