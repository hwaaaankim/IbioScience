package com.dev.IbioScience.model.product.status;

//공통질문 입력타입 - 입력형태 구분
public enum QuestionType {
	INPUT("일반 입력란"), // 단일 입력란
	TEXTAREA("여러줄 입력란"), // 다중라인 입력란
	SELECT("선택(콤보박스)"), // 선택형(드롭다운)
	FILE("파일 업로드"), // 파일 업로드
	CKEDITOR("CKEditor(HTML)"); // 에디터(HTML)

	private final String label;

	QuestionType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
