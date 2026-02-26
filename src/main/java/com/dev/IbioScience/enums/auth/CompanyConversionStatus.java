package com.dev.IbioScience.enums.auth;

public enum CompanyConversionStatus {
	PENDING,    // 신청 대기
	APPROVED,   // 승인
	REJECTED,   // 반려
	EXPIRED,    // 만료(정책상 자동 종료 등)
	CANCELED    // 신청자 취소(추후 필요 시)
}