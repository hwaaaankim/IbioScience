package com.dev.IbioScience.model.product.status;

//쿠폰정책 - 어떤 정책을 쓸지
public enum CouponPolicy {
	ALL("전체"), // 전체 대상
	SPECIFIC("특정조건"), // 특정 조건
	NONE("없음"); // 쿠폰 없음

	private final String label;

	CouponPolicy(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}