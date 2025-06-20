package com.dev.IbioScience.model.product;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

//제품-키워드 매핑
@Data
@Entity
@Table(name = "tb_product_keyword", uniqueConstraints = @UniqueConstraint(columnNames = { "product_id", "keyword_id" }))
public class ProductKeyword {
	
	// 매핑 ID, PK
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 소속 제품
	@ManyToOne(fetch = FetchType.LAZY)
	private Product product;

	// 소속 키워드
	@ManyToOne(fetch = FetchType.LAZY)
	private Keyword keyword;
}