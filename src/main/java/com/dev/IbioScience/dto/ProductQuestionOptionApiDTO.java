package com.dev.IbioScience.dto;

import com.dev.IbioScience.model.product.ProductQuestionOption;

import lombok.Data;

@Data
public class ProductQuestionOptionApiDTO {
	private Long id;
	private String value;
	private Integer sortOrder;

	public static ProductQuestionOptionApiDTO from(ProductQuestionOption o) {
		ProductQuestionOptionApiDTO d = new ProductQuestionOptionApiDTO();
		d.setId(o.getId());
		d.setValue(o.getValue());
		d.setSortOrder(o.getSortOrder());
		return d;
	}
}