package com.dev.IbioScience.model.product;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

//옵션 그룹 엔티티
@Data
@Entity
@Table(name = "tb_product_option_group")
public class ProductOptionGroup {
	
	// 옵션 그룹 ID, PK
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 소속 제품
	@ManyToOne(fetch = FetchType.LAZY)
	private Product product;

	// 옵션 그룹명
	private String name;
	// 정렬순서
	private Integer sortOrder;

	// 옵션 리스트
	@OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductOption> options = new ArrayList<>();
}
