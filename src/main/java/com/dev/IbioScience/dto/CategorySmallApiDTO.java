package com.dev.IbioScience.dto;

import com.dev.IbioScience.model.product.category.CategorySmall;

import lombok.Data;

@Data
public class CategorySmallApiDTO {
	private Long id;
	private String name;

	public static CategorySmallApiDTO from(CategorySmall e) {
		CategorySmallApiDTO d = new CategorySmallApiDTO();
		d.setId(e.getId());
		d.setName(e.getName());
		return d;
	}
}