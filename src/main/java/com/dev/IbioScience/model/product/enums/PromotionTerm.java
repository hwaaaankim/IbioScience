package com.dev.IbioScience.model.product.enums;

//할인기간 정책 - 한정/상시
public enum PromotionTerm {
	PERIOD("기간한정"), // 기간 한정
	ALWAYS("상시"); // 상시

	private final String label;

	PromotionTerm(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}