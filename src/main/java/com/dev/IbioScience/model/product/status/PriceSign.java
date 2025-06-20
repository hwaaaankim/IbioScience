package com.dev.IbioScience.model.product.status;

//옵션 추가금액 부호 - 추가/차감
public enum PriceSign {
	PLUS("추가"), // 추가금액(+)
	MINUS("차감"); // 차감금액(-)

	private final String label;

	PriceSign(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}