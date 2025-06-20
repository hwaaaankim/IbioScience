package com.dev.IbioScience.model.product.status;

//상품 이미지에서 사용 - 대표/추가
public enum ProductImageType {
	MAIN("대표"), // 대표 이미지
	ADDITIONAL("추가"); // 추가 이미지

	private final String label;

	ProductImageType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}