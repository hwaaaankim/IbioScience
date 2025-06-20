package com.dev.IbioScience.model.product.status;

//할인/증정 정책 타입 - 할인인지 증정인지
public enum DiscountType {
	DISCOUNT("할인"), // 할인
	GIFT("증정"); // 증정

	private final String label;

	DiscountType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}