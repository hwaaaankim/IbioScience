package com.dev.IbioScience.model.product.status;

//가격 노출 정책 - 누구에게 가격을 보여줄지
public enum PriceExposure {
	NONE("노출안함"), // 가격 노출 안함
	ALL("전체노출"), // 전체에게 노출
	MEMBER_ONLY("회원만 노출"); // 회원만 노출

	private final String label;

	PriceExposure(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}