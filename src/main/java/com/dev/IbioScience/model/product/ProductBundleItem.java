package com.dev.IbioScience.model.product;

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
@Table(name = "tb_product_bundle_item")
public class ProductBundleItem {
	
	// 번들항목 ID, PK
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 메인 제품
	@ManyToOne(fetch = FetchType.LAZY)
	private Product mainProduct;

	// 구성상품
	@ManyToOne(fetch = FetchType.LAZY)
	private Product bundleProduct;

	// 정렬순서
	private Integer sortOrder;
}
