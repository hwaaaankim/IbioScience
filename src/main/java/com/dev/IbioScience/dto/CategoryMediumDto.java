package com.dev.IbioScience.dto;

import com.dev.IbioScience.model.product.category.CategoryMedium;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryMediumDto {
	private Long id;
	private String name;
	private Long largeId;

	public static CategoryMediumDto from(CategoryMedium m) {
		return new CategoryMediumDto(m.getId(), m.getName(), m.getLarge().getId());
	}
}