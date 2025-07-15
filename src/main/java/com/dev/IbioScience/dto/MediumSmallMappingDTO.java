package com.dev.IbioScience.dto;

import com.dev.IbioScience.model.product.relation.MediumSmallCategory;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MediumSmallMappingDTO {
	private Long id;
	private Long mediumId;
	private Long smallId;

	public static MediumSmallMappingDTO from(MediumSmallCategory m) {
		return new MediumSmallMappingDTO(m.getId(), m.getMedium().getId(), m.getSmall().getId());
	}
}