package com.dev.IbioScience.dto;

import com.dev.IbioScience.model.product.ProductQuestionOption;

import lombok.Data;

@Data
public class ProductQuestionOptionApiDto {
	private Long id;
	private String value;
	private Integer sortOrder;

	public static ProductQuestionOptionApiDto from(ProductQuestionOption o) {
		ProductQuestionOptionApiDto d = new ProductQuestionOptionApiDto();
		d.setId(o.getId());
		d.setValue(o.getValue());
		d.setSortOrder(o.getSortOrder());
		return d;
	}
}