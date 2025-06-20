package com.dev.IbioScience.model.product;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

//브랜드 엔티티
@Data
@Entity
@Table(name = "tb_brand")
public class Brand {
	
	// 브랜드 ID, PK
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	// 브랜드명
	private String name;
}
