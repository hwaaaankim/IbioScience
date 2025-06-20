package com.dev.IbioScience.model.product.status;

//할인기간 정책 - 한정/상시
public enum DiscountTerm {
	PERIOD("기간한정"), // 기간 한정
	ALWAYS("상시"); // 상시

	private final String label;

	DiscountTerm(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}