package com.dev.IbioScience.model.product;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

//공통질문 선택지 옵션
@Data
@Entity
@Table(name = "tb_product_question_option")
public class ProductQuestionOption {
	
	// 옵션 ID, PK
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 소속 질문
	@ManyToOne(fetch = FetchType.LAZY)
	private ProductQuestion question;

	// 옵션값
	private String value;

	// 정렬순서
	private Integer sortOrder;
}