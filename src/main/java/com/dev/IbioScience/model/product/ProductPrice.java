package com.dev.IbioScience.model.product;

import java.math.BigDecimal;

import com.dev.IbioScience.model.product.status.PriceExposure;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

//제품별 가격정책
@Data
@Entity
@Table(name = "tb_product_price")
public class ProductPrice {
	
	// 가격 ID, PK
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 소속 제품(1:1)
	@OneToOne(fetch = FetchType.LAZY)
	private Product product;

	// 가격
	private BigDecimal price;

	// 노출정책(NONE/ALL/MEMBER_ONLY)
	@Enumerated(EnumType.STRING)
	private PriceExposure exposure;

	// 대체라벨 사용여부
	private Boolean useAlternativeLabel;

	// 대체라벨(예: "상담문의")
	private String alternativeLabel;

	// 적립금 비율
	private BigDecimal pointRate;
}