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

//상품 상세이미지(업로드용)
@Data
@Entity
@Table(name = "tb_product_detail_image")
public class ProductDetailImage {

	// 상세이미지 ID, PK
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 소속 제품
	@ManyToOne(fetch = FetchType.LAZY)
	private Product product;

	// 파일 경로/URL
	private String url;
	
	// 파일 저장 PATH
	private String path;
	
	// 파일 이름
	private String fileName;

	// 원본 파일명
	private String originalFilename;

	// 파일크기(byte)
	private Integer size;

	// 업로드 일시
	private LocalDateTime uploadedAt;

	// 상세설명(HTML) 실제 사용중 여부
	private Boolean inUse;

	// 정렬순서
	private Integer sortOrder;
}