package com.dev.IbioScience.model.product.category;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "tb_category_large")
public class CategoryLarge {
    
	// 대분류 ID, PK
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 대분류명
    private String name;

    // 소속 중분류 리스트
    @OneToMany(mappedBy = "large", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CategoryMedium> mediums = new ArrayList<>();
}
