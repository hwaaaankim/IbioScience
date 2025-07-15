package com.dev.IbioScience.dto;

import com.dev.IbioScience.model.product.category.CategoryLarge;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryLargeDTO {
	private Long id;
	private String name;

	public static CategoryLargeDTO from(CategoryLarge l) {
		return new CategoryLargeDTO(l.getId(), l.getName());
	}
}