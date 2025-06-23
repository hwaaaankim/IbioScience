package com.dev.IbioScience.dto;

import com.dev.IbioScience.model.product.category.CategorySmall;

import lombok.Data;

@Data
public class CategorySmallApiDto {
	private Long id;
	private String name;

	public static CategorySmallApiDto from(CategorySmall e) {
		CategorySmallApiDto d = new CategorySmallApiDto();
		d.setId(e.getId());
		d.setName(e.getName());
		return d;
	}
}