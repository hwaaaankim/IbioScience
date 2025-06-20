package com.dev.IbioScience.model.product;

import java.time.LocalDateTime;

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
@Table(name = "tb_product_answer_detail_image")
public class ProductAnswerDetailImage {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 어떤 답변의 이미지인지
	@ManyToOne(fetch = FetchType.LAZY)
	private ProductAnswer answer;

	private String url;
	private String path;
	private String fileName;
	private String originalFilename;
	private Integer size;
	private LocalDateTime uploadedAt;
	private Boolean inUse; // value 내에서 실제 사용중인지
	private Integer sortOrder;
}