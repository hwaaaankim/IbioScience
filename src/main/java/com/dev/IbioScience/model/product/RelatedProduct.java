package com.dev.IbioScience.model.product;

import com.dev.IbioScience.model.product.status.RelatedType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

//연관상품 엔티티
@Data
@Entity
@Table(name = "tb_related_product", 
	uniqueConstraints = @UniqueConstraint(columnNames = { "baseProduct_id", "relatedProduct_id", "type" }))
public class RelatedProduct {
	
	// 연관상품 ID, PK
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 기준제품
	@ManyToOne(fetch = FetchType.LAZY)
	private Product baseProduct;

	// 연관제품
	@ManyToOne(fetch = FetchType.LAZY)
	private Product relatedProduct;

	// 연관 타입(ONEWAY/RECIPROCAL)
	@Enumerated(EnumType.STRING)
	private RelatedType type;

	// 정렬순서
	private Integer sortOrder;
}