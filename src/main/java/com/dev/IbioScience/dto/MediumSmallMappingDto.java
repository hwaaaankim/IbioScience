package com.dev.IbioScience.dto;

import com.dev.IbioScience.model.product.relation.MediumSmallCategory;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MediumSmallMappingDto {
	private Long id;
	private Long mediumId;
	private Long smallId;

	public static MediumSmallMappingDto from(MediumSmallCategory m) {
		return new MediumSmallMappingDto(m.getId(), m.getMedium().getId(), m.getSmall().getId());
	}
}