package com.dev.IbioScience.model.product;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

//상품별 추가입력 필드(질문/답변)
@Data
@Entity
@Table(name = "tb_product_extra_field")
public class ProductExtraField {
	
	// 추가입력 필드 ID, PK
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 소속 제품
	@ManyToOne(fetch = FetchType.LAZY)
	private Product product;

	// 질문명
	private String label;

	// 답변값
	private String value;
}