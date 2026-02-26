package com.dev.IbioScience.dto.customer.auth;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WithdrawMemberRowDto {

	private final Long memberId;
	private final String username;
	private final String companyName; // 개인: "- 없음 -"
	private final String name;
	private final String contact;     // mobile 우선, 없으면 tel
	private final LocalDateTime requestedAt; // withdrewAt 기준
}