package com.dev.IbioScience.model.product.status;

//연관상품 타입 - 단방향/쌍방향
public enum RelatedType {
	ONEWAY("단방향"), // 단방향 연관
	RECIPROCAL("쌍방향"); // 쌍방향 연관

	private final String label;

	RelatedType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}