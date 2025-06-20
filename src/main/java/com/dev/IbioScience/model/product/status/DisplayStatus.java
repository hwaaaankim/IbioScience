package com.dev.IbioScience.model.product.status;

//진열상태 - ON/OFF
public enum DisplayStatus {
	ON("진열함"), // 진열중
	OFF("진열안함"); // 진열안함

	private final String label;

	DisplayStatus(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
