package com.dev.IbioScience.model.product;

import java.time.LocalDate;

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
@Table(name = "tb_product_icon")
public class ProductIcon {
	
	// 아이콘 ID, PK
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 아이콘 이미지 URL
	private String url;
	
	// 저장 경로
    private String path;
    
    // 파일 이름
    private String fileName;

	// 시작일
	private LocalDate startDate;

	// 종료일
	private LocalDate endDate;

	// 소속 제품
	@ManyToOne(fetch = FetchType.LAZY)
	private Product product;
}
