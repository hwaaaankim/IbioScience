package com.dev.IbioScience.enums.product;

//진열상태 - ON/OFF
public enum DisplayStatus {
	
	ON("진열함"), // 진열중
	OFF("진열안함"); // 진열안함
	// TEMP("임시 진열 상품"); // 기간 진열 상품 예시

	private final String label;

	DisplayStatus(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
