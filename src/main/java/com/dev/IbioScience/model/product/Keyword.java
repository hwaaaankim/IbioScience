package com.dev.IbioScience.model.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

//키워드(검색어)
@Data
@Entity
@Table(name = "tb_keyword")
public class Keyword {
	
	// 키워드 ID, PK
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 키워드 텍스트(중복불가)
	@Column(unique = true)
	private String word;
}