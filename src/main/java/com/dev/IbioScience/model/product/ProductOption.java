package com.dev.IbioScience.model.product;

import java.math.BigDecimal;

import com.dev.IbioScience.model.product.status.PriceSign;

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

//옵션 엔티티
@Data
@Entity
@Table(name = "tb_product_option")
public class ProductOption {
	
	// 옵션 ID, PK
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 소속 옵션그룹
	@ManyToOne(fetch = FetchType.LAZY)
	private ProductOptionGroup group;

	// 옵션명(예: 50ml)
	private String name;
	
	// 옵션값(내부값)
	private String value;
	
	// 추가금액
	private BigDecimal extraPrice;
	
	// 금액부호(PLUS, MINUS)
	@Enumerated(EnumType.STRING)
	private PriceSign sign;
	
	// 정렬순서
	private Integer sortOrder;
}
