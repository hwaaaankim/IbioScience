package com.dev.IbioScience.model.product.status;

//회원 등급 구분
public enum MemberGrade {
	ALL("전체"), // 전체 회원
	NORMAL("일반회원"), // 일반회원
	DEALER("딜러회원"); // 딜러회원

	private final String label;

	MemberGrade(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}