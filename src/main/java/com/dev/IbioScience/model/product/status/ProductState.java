package com.dev.IbioScience.model.product.status;

//상품상태 - 정상/삭제대기/삭제됨
public enum ProductState {
	NORMAL("정상"), // 정상
	WAITING_DELETE("삭제대기"), // 삭제대기
	DELETED("삭제됨"); // 삭제됨

	private final String label;

	ProductState(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
