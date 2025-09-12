package com.dev.IbioScience.model.auth.utils;

import com.dev.IbioScience.model.auth.enums.DealerType;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DealerApplyRequest {
	@NotNull
	private Long memberId; // 신청자 ID (프론트에서 전달)
	@NotNull
	private DealerType targetDealerType; // BUYER 또는 SELLER
	private String note; // 신청 메모(선택)
}