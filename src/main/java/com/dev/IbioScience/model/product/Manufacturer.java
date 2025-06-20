package com.dev.IbioScience.model.product;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

//제조사 엔티티
@Data
@Entity
@Table(name = "tb_manufacturer")
public class Manufacturer {
	
	// 제조사 ID, PK
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	// 제조사명
	private String name;
}
