package com.dev.IbioScience.enums.product;

//상품 신상상태 ENUM
public enum ProductNewState {
	NEW("신상품"), // 신상품
	STOCK("재고상품"), // 재고상품
	DISPLAY("전시상품"); // 전시상품

	private final String label;

	ProductNewState(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}