package com.dev.IbioScience.dto;

import com.dev.IbioScience.model.product.category.CategoryMedium;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryMediumDTO {
	private Long id;
	private String name;
	private Long largeId;

	public static CategoryMediumDTO from(CategoryMedium m) {
		return new CategoryMediumDTO(m.getId(), m.getName(), m.getLarge().getId());
	}
}