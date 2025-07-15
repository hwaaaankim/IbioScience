package com.dev.IbioScience.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.dev.IbioScience.model.product.ProductQuestion;
import com.dev.IbioScience.model.product.ProductQuestionOption;

import lombok.Data;

@Data
public class ProductQuestionApiDTO {
	private Long id;
	private String label;
	private String type;
	private Boolean required;
	private String placeholder;
	private List<ProductQuestionOptionApiDTO> options;
	private Integer sortOrder;

	public static ProductQuestionApiDTO from(ProductQuestion q, List<ProductQuestionOption> opts) {
		ProductQuestionApiDTO dto = new ProductQuestionApiDTO();
		dto.setId(q.getId());
		dto.setLabel(q.getLabel());
		dto.setType(q.getType().name());
		dto.setRequired(q.getRequired());
		dto.setPlaceholder(q.getPlaceholder());
		dto.setSortOrder(q.getSortOrder());
		dto.setOptions(opts.stream().map(ProductQuestionOptionApiDTO::from).collect(Collectors.toList()));
		return dto;
	}
}