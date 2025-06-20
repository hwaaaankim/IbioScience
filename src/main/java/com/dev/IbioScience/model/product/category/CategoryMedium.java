package com.dev.IbioScience.model.product.category;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "tb_category_medium")
public class CategoryMedium {
    
	// 중분류 ID, PK
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 중분류명
    private String name;
   
    // 소속 대분류 참조
    @ManyToOne(fetch = FetchType.LAZY)
    private CategoryLarge large;
}