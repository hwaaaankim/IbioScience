package com.dev.IbioScience.dto;

import com.dev.IbioScience.model.product.category.CategoryLarge;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryLargeDto {
	private Long id;
	private String name;

	public static CategoryLargeDto from(CategoryLarge l) {
		return new CategoryLargeDto(l.getId(), l.getName());
	}
}