package com.dev.IbioScience.model.product.category;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

//소분류 카테고리
@Data
@Entity
@Table(name = "tb_category_small")
public class CategorySmall {
	
	// 소분류 ID, PK
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	// 소분류명
	private String name;
}
