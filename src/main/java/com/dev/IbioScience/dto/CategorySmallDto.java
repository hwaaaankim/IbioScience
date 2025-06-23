package com.dev.IbioScience.dto;

import com.dev.IbioScience.model.product.category.CategorySmall;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategorySmallDto {
	private Long id;
	private String name;

	public static CategorySmallDto from(CategorySmall s) {
		return new CategorySmallDto(s.getId(), s.getName());
	}
}