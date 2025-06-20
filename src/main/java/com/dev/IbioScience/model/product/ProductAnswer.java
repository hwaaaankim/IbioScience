package com.dev.IbioScience.model.product;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

//공통질문 답변
@Data
@Entity
@Table(name = "tb_product_answer")
public class ProductAnswer {
	
	// 답변 ID, PK
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 소속 제품
	@ManyToOne(fetch = FetchType.LAZY)
	private Product product;

	// 소속 질문
	@ManyToOne(fetch = FetchType.LAZY)
	private ProductQuestion question;

	// 답변 값(텍스트/HTML)
	@Column(columnDefinition = "TEXT")
	private String value;

	// 파일 타입일 경우 파일 ROAD 경로
	private String fileUrl;
	
	// 파일 타입일 경우 파일 저장 경로
	private String path;
	
	// 파일명
	private String fileName;
	
	@OneToMany(mappedBy = "answer", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductAnswerDetailImage> detailImages = new ArrayList<>();
}