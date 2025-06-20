package com.dev.IbioScience.model.product.status;

//판매상태 - ON/OFF
public enum SaleStatus {
	ON("판매함"), // 판매중
	OFF("판매안함"); // 판매안함

	private final String label;

	SaleStatus(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
