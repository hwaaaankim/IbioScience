package com.dev.IbioScience.model.product.relation;

import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

//중분류-소분류 N:N 관계 (중간 테이블)
@Data
@Entity
@Table(name = "tb_medium_small_category", uniqueConstraints = @UniqueConstraint(columnNames = {"medium_id", "small_id"}))
public class MediumSmallCategory {
    
	// PK
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 중분류 참조
    @ManyToOne(fetch = FetchType.LAZY)
    private CategoryMedium medium;
    
    // 소분류 참조
    @ManyToOne(fetch = FetchType.LAZY)
    private CategorySmall small;
    
    // 정렬순서(선택)
    private Integer sortOrder;
}
