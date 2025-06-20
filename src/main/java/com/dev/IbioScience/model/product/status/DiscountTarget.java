package com.dev.IbioScience.model.product.status;

//할인대상 - 누구에게 적용할지
public enum DiscountTarget {
	ALL("전체"), // 전체
	NORMAL("일반"), // 일반회원
	DEALER("딜러"); // 딜러회원

	private final String label;

	DiscountTarget(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
