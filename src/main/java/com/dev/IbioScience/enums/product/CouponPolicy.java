package com.dev.IbioScience.enums.product;

//쿠폰정책 - 어떤 정책을 쓸지
public enum CouponPolicy {
	ALL("전체"), // 전체 대상
	SPECIFIC("딜러"), // 특정 조건
	NONE("일반"); // 쿠폰 없음

	private final String label;

	CouponPolicy(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}