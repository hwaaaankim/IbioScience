package com.dev.IbioScience.model.product;

import com.dev.IbioScience.model.product.status.ProductImageType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

//상품 이미지(대표/추가)
@Data
@Entity
@Table(name = "tb_product_image")
public class ProductImage {
	
	// 이미지 ID, PK
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 소속 제품
	@ManyToOne(fetch = FetchType.LAZY)
	private Product product;

	// 이미지 타입(MAIN, ADDITIONAL)
	@Enumerated(EnumType.STRING)
	private ProductImageType type;
	
	// 파일 ROAD 경로
    private String url;
    
    // 파일 저장 경로 PATH
    private String path;
    
    // 파일명
    private String fileName; 
	
	// 정렬순서
	private Integer sortOrder;
}