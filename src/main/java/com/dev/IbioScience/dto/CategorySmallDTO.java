package com.dev.IbioScience.dto;

import com.dev.IbioScience.model.product.category.CategorySmall;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategorySmallDTO {
	private Long id;
	private String name;

	public static CategorySmallDTO from(CategorySmall s) {
		return new CategorySmallDTO(s.getId(), s.getName());
	}
}