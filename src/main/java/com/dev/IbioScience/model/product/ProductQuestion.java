package com.dev.IbioScience.model.product;

import java.util.ArrayList;
import java.util.List;

import com.dev.IbioScience.model.product.status.QuestionType;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

//상품 공통 질문/옵션 엔티티
@Data
@Entity
@Table(name = "tb_product_question")
public class ProductQuestion {
	
	// 질문/옵션 ID, PK
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 라벨(질문/옵션명)
	private String label;

	// 질문타입(INPUT, TEXTAREA, SELECT, FILE, CKEDITOR)
	@Enumerated(EnumType.STRING)
	private QuestionType type;

	// 필수 여부
	private Boolean required;

	// placeholder
	private String placeholder;

	// 정렬순서
	private Integer sortOrder;

	// 선택지 옵션 리스트(SELECT 등)
	@OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference
	private List<ProductQuestionOption> options = new ArrayList<>();

	// 답변 리스트
	@OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductAnswer> answers = new ArrayList<>();
}
