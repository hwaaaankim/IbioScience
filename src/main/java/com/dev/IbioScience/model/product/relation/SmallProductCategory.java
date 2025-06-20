package com.dev.IbioScience.model.product.relation;

import com.dev.IbioScience.model.product.Product;
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

//소분류-제품 N:N 관계 (중간 테이블)
@Data
@Entity
@Table(name = "tb_small_product_category", 
		uniqueConstraints = @UniqueConstraint(columnNames = { "small_id", "product_id" }))
public class SmallProductCategory {
	
	// PK
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	// 소분류 참조
	@ManyToOne(fetch = FetchType.LAZY)
	private CategorySmall small;
	// 제품 참조
	@ManyToOne(fetch = FetchType.LAZY)
	private Product product;
	// 정렬순서(선택)
	private Integer sortOrder;
}