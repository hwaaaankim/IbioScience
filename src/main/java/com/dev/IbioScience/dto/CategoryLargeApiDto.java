package com.dev.IbioScience.dto;

import com.dev.IbioScience.model.product.category.CategoryLarge;

import lombok.Data;

@Data
public class CategoryLargeApiDto {
	private Long id;
	private String name;

	public static CategoryLargeApiDto from(CategoryLarge e) {
		CategoryLargeApiDto d = new CategoryLargeApiDto();
		d.setId(e.getId());
		d.setName(e.getName());
		return d;
	}
}